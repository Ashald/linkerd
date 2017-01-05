package io.buoyant.telemetry

import com.twitter.finagle.Stack
import com.twitter.finagle.util.LoadService
import com.twitter.util._
import io.buoyant.config.Parser
import org.scalatest._

class StatsDInitializerTest extends FunSuite {

  test("io.l5d.default telemeter loads") {
    val yaml =
      """|kind: io.l5d.statsd
         |sampleRate: 0.02
         |""".stripMargin

    val config = Parser.objectMapper(yaml, Seq(LoadService[TelemeterInitializer]))
      .readValue[TelemeterConfig](yaml)

    val telemeter = config.mk(Stack.Params.empty)
    assert(telemeter.stats.isNull)
    assert(!telemeter.tracer.isNull)
  }

  test("io.l5d.default telemeter loads, with log level") {
    val yaml =
      """|kind: io.l5d.statsd
         |level: trace
         |""".stripMargin

    val config = Parser.objectMapper(yaml, Seq(LoadService[TelemeterInitializer]))
      .readValue[TelemeterConfig](yaml)

    val telemeter = config.mk(Stack.Params.empty)
    assert(telemeter.stats.isNull)
    assert(!telemeter.tracer.isNull)
  }

  test("io.l5d.default telemeter fails with invalid log level") {
    val yaml =
      """|kind: io.l5d.statsd
         |level: supergood
         |""".stripMargin

    val mapper = Parser.objectMapper(yaml, Seq(LoadService[TelemeterInitializer]))
    val _ = intercept[com.fasterxml.jackson.databind.JsonMappingException] {
      mapper.readValue[TelemeterConfig](yaml)
    }
  }

}
