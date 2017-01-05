package io.buoyant.telemetry.statsd

import com.timgroup.statsd.NonBlockingStatsDClient
import com.twitter.conversions.time._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.{Failure, Path, Stack}
import com.twitter.logging.Logger
import com.twitter.util.{Awaitable, Closable, CloseAwaitably, Future, Time}
import io.buoyant.telemetry._
import java.util.concurrent.atomic.AtomicBoolean

private[statsd] object StatsDTelemeter {
  val MaxQueueSize = 10000

  val Closed = Failure("closed", Failure.Interrupted)

  val ProcessId = Path.Utf8("l5d", "uuid", java.util.UUID.randomUUID().toString)
}

private[telemetry] class StatsDTelemeter(
  logger: Logger,
  prefix: String,
  hostname: String,
  port: Int,
  gaugePeriodMs: Int
) extends Telemeter {
  import StatsDTelemeter._

  // prefix format is: "prefix_l5d-process-id"
  private[this] val statsDClient = new NonBlockingStatsDClient(
    prefix + ProcessId.showElems.mkString("_", "-", ""),
    hostname,
    port,
    MaxQueueSize
  )

  val stats = new StatsDStatsReceiver(statsDClient)

  // no tracer with statsd
  val tracer = NullTracer

  private[this] val started = new AtomicBoolean(false)

  private[this] val timer = DefaultTimer.twitter

  // Only run at most once.
  def run(): Closable with Awaitable[Unit] =
    if (started.compareAndSet(false, true)) run0()
    else Telemeter.nopRun

  private[this] def run0() = {
    var pendingF = Future.Unit
    val task = timer.schedule(gaugePeriodMs.millis) {
      val rspF = stats.flushGauges.unit
      // Chain pending so long-pending requests may be canceled as well.
      pendingF = pendingF.before(rspF)
    }

    val closer = Closable.sequence(task)

    new Closable with CloseAwaitably {
      def close(deadline: Time) = closeAwaitably {
        statsDClient.stop()
        pendingF.raise(Closed)
        closer.close(deadline)
      }
    }
  }
}
