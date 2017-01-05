package io.buoyant.telemetry.statsd

import java.util.concurrent.ConcurrentHashMap
import com.timgroup.statsd.StatsDClient
import com.twitter.finagle.stats.{Counter, Stat, StatsReceiverWithCumulativeGauges}
import com.twitter.util.Future
import scala.collection.JavaConverters._

private[statsd] class StatsDStatsReceiver(statsDClient: StatsDClient)
  extends StatsReceiverWithCumulativeGauges {

  val repr: AnyRef = this

  // from https://github.com/researchgate/diamond-linkerd-collector/
  private[this] def mkName(name: Seq[String]): String = {
    name.mkString("/")
      .replace(".", "_")
      .replace("#", "")
      .replace("//", "/")
      .replace("/", ".") // http://graphite.readthedocs.io/en/latest/feeding-carbon.html#step-1-plan-a-naming-hierarchy
  }

  private[this] val gauges = new ConcurrentHashMap[String, Metric.Gauge].asScala

  private[statsd] def flushGauges(): Future[Unit] = {
    Future.value(gauges.values.foreach(_.send))
  }

  protected[this] def registerGauge(name: Seq[String], f: => Float): Unit = {
    val statsDName = mkName(name)
    gauges(mkName(name)) = new Metric.Gauge(statsDClient, statsDName, f)
  }

  protected[this] def deregisterGauge(name: Seq[String]): Unit = {
    val _ = gauges.remove(mkName(name))
  }

  def counter(name: String*): Counter =
    new Metric.Counter(statsDClient, mkName(name))

  def stat(name: String*): Stat =
    new Metric.Stat(statsDClient, mkName(name))
}
