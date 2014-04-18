package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import Fixtures._
import org.specs2.mock.Mockito
import au.com.dius.pact.model.{Response, Interaction}
import au.com.dius.pact.consumer.PactVerification._
import scala.util.{Failure => TFailure, Success => TSuccess, Try}
import scalaz.{Failure, Success}

class PactVerificationSpec extends Specification with Mockito {
  "PactVerification" should {
    def test(
      actualInteractions: Iterable[Interaction],
      expectedResult: VerificationResult,
      testPassed: Try[Unit] = TSuccess(Unit)) = {
        PactVerification(pact.interactions, actualInteractions, testPassed) must beEqualTo(expectedResult)
    }

    val invalidResponse = Response(500, None, None)
    val unexpectedInteraction = Interaction("", "", request.copy(path = "unexpected"), invalidResponse)

    "fail fast if tests didn't pass" in {
      val error = new RuntimeException("bad things")
      test(
        Seq(),
        ConsumerTestsFailed(error),
        TFailure(error)
      )
    }

    "complain about missing interactions" in {
      test(
        Seq(),
        MissingInteractions(pact.interactions)
      )
    }

    "complain about unexpected interactions" in {
      test(
        interactions :+ unexpectedInteraction,
        UnexpectedInteractions(Seq(unexpectedInteraction))
      )
    }

    "complain about both failures" in {
      val actual = Seq(unexpectedInteraction)
      test(
        actual,
        Failure(Seq(MissingInteractions(pact.interactions), UnexpectedInteractions(actual)))
      )
    }

    "pass successful runs" in {
      test(
        interactions.map(_.copy(description="MockServiceProvider received")),
        Success()
      )
    }
  }
}
