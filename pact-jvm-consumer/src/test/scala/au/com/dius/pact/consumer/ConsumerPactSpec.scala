package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.model.Pact
import Fixtures._
import au.com.dius.pact.model.{MakePact, MakeInteraction}
import MakeInteraction._
import ConsumerPact._
import au.com.dius.pact.consumer.PactVerification.ConsumerTestsFailed
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}
import scalaz.{Failure, Success}

/**
 * This is what a consumer pact should roughly look like
 */
class ConsumerPactSpec extends Specification {

  isolated

  "consumer pact" should {
    val pact: Pact = MakePact()
    .withProvider(provider.name)
    .withConsumer(consumer.name)
    .withInteractions(
      given(interaction.providerState)
    .uponReceiving(
        description = interaction.description,
        path = request.path,
        method = request.method,
        headers = request.headers,
        body = request.body)
    .willRespondWith(status = 200, headers = response.headers, body = response.body))

    def awaitResult[A](f: Future[A]): A = {
      Await.result(f, Duration(10, "s"))
    }

    "Report test success and write pact" in {
      val config = PactServerConfig()

      awaitResult(pact.runConsumer(config, interaction.providerState) {
        awaitResult(ConsumerService(config.url).hitEndpoint) must beTrue
      }) must beEqualTo(Success())

      //TODO: use environment property for pact output folder
      val saved: String = io.Source.fromFile(s"target/pacts/${pact.consumer.name}-${pact.provider.name}.json").mkString
      val savedPact = Pact.from(saved)

      //TODO: update expected string when pact serialization is complete
      savedPact must beEqualTo(pact)
    }

    "Report test failure nicely" in {
      val error = new RuntimeException("bad things happened in the test!")
      val config = PactServerConfig()
      awaitResult(pact.runConsumer(config, interaction.providerState) {
        throw error
      }) must beEqualTo(Failure(Seq(ConsumerTestsFailed(error))))
    }
  }
}