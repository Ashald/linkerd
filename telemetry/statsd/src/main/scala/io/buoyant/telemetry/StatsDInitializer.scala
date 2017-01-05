package io.buoyant.telemetry

import com.twitter.finagle.Stack
import com.twitter.logging.Logger
import io.buoyant.telemetry.statsd.StatsDTelemeter

class StatsDInitializer extends TelemeterInitializer {
  type Config = StatsDConfig
  val configClass = classOf[StatsDConfig]
  override val configId = "io.l5d.statsd"
}

case class StatsDConfig(
  prefix: Option[String],
  hostname: Option[String],
  port: Option[Int],
  gaugePeriodMs: Option[Int]
) extends TelemeterConfig {

  private[this] val logger = Logger.get("io.l5d.statsd")

  def mk(params: Stack.Params): StatsDTelemeter =
    new StatsDTelemeter(
      logger,
      prefix.getOrElse("linkerd"),
      hostname.getOrElse("127.0.0.1"),
      port.getOrElse(8125),
      gaugePeriodMs.getOrElse(10000)
    )
}
