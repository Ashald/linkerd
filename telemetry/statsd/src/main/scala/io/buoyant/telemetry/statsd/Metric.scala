package io.buoyant.telemetry.statsd

import com.timgroup.statsd.StatsDClient
import com.twitter.finagle.stats.{Counter => FCounter, Stat => FStat}

private[statsd] object Metric {

  class Counter(statsDClient: StatsDClient, name: String) extends FCounter {
    def incr(delta: Int): Unit = {
      statsDClient.count(name, delta)
    }
  }

  class Stat(statsDClient: StatsDClient, name: String) extends FStat {
    def add(value: Float): Unit =
      // would prefer `recordHistogramValue`, but that is Datadog specific
      statsDClient.recordExecutionTime(name, value.toLong)
  }

  class Gauge(statsDClient: StatsDClient, name: String, f: => Float) {
    def send: Unit = {
      statsDClient.recordGaugeValue(name, f)
    }
  }
}
