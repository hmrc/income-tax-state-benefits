/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package actions

import config.AppConfig
import models.authorisation.Enrolment.{Agent, Individual, Nino, SupportingAgent}
import models.requests.AuthorisationRequest
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.SystemMaterializer
import org.scalamock.handlers.{CallHandler0, CallHandler4}
import play.api.http.Status._
import play.api.http.{HeaderNames, Status => TestStatus}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test._
import support.UnitTest
import support.mocks.MockAuthConnector
import support.providers.FakeRequestProvider
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends UnitTest
  with MockAuthConnector
  with FutureAwaits with DefaultAwaitTimeout
  with FakeRequestProvider
  with HeaderNames with TestStatus with ResultExtractors {

  private val requestWithMtditid: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withHeaders("mtditid" -> "1234567890")
  private implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  private val defaultActionBuilder: DefaultActionBuilder = DefaultActionBuilder(mockControllerComponents.parsers.default)
  implicit val materializer: SystemMaterializer = SystemMaterializer(actorSystem)

  private val underTest: AuthorisedAction = new AuthorisedAction(defaultActionBuilder, mock[AppConfig], mockAuthConnector, mockControllerComponents)

  trait AgentTest {
    val nino = "AA111111A"
    val mtdItId: String = "1234567890"
    val arn: String = "0987654321"

    val validHeaderCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))

    val testBlock: AuthorisationRequest[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.user.mtditid} ${user.user.arn.get}"))

    val mockAppConfig: AppConfig = mock[AppConfig]

    def primaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth")

    def secondaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT-SUPP")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth-supp")

    def mockMultipleAgentsSwitch(bool: Boolean): CallHandler0[Boolean] =
      (mockAppConfig.emaSupportingAgentsEnabled _: () => Boolean)
        .expects()
        .returning(bool)
        .anyNumberOfTimes()

    val primaryAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtdItId)), "Activated"),
      Enrolment(Agent.key, Seq(EnrolmentIdentifier(Agent.value, arn)), "Activated")
    ))

    val supportingAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(SupportingAgent.key, Seq(EnrolmentIdentifier(Individual.value, mtdItId)), "Activated"),
      Enrolment(Agent.key, Seq(EnrolmentIdentifier(Agent.value, arn)), "Activated")
    ))

    def mockAuthReturnException(exception: Exception,
                                predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.failed(exception))

    def mockAuthReturn(enrolments: Enrolments, predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.successful(enrolments))

    def testAuth: AuthorisedAction = new AuthorisedAction(
      defaultActionBuilder = defaultActionBuilder,
      appConfig = mockAppConfig,
      authConnector = mockAuthConnector,
      cc = mockControllerComponents
    )

    lazy val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
      "mtditid" -> mtdItId
    )
  }

  ".async" should {
    lazy val block: AuthorisationRequest[AnyContent] => Future[Result] = request =>
      Future.successful(Ok(s"mtditid: ${request.user.mtditid}${request.user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {
      "the user is successfully verified as an agent" which {
        val agentEnrolments: Enrolments = Enrolments(Set(
          Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, "1234567890")), "Activated"),
          Enrolment(Agent.key, Seq(EnrolmentIdentifier(Agent.value, "0987654321")), "Activated")
        ))

        mockAuthAsAgent(agentEnrolments)

        val result = await(underTest.async(block)(requestWithMtditid))

        "should return an OK(200) status" in {
          result.header.status shouldBe OK
          await(result.body.consumeData.map(_.utf8String)) shouldBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {
        val individualEnrolments: Enrolments = Enrolments(Set(
          Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, "1234567890")), "Activated"),
          Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, "1234567890")), "Activated")
        ))

        mockAuth(individualEnrolments)

        lazy val result = await(underTest.async(block)(requestWithMtditid))

        result.header.status shouldBe OK
        await(result.body.consumeData.map(_.utf8String)) shouldBe "mtditid: 1234567890"
      }
    }

    "return an Unauthorised" when {
      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        mockAuthReturnException(AuthException)

        await(underTest.async(block)(requestWithMtditid)).header.status shouldBe UNAUTHORIZED
      }
    }

    "return an Unauthorised" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        mockAuthReturnException(NoActiveSession)

        await(underTest.async(block)(requestWithMtditid)).header.status shouldBe UNAUTHORIZED
      }

      "the request does not contain mtditid header" in {
        await(underTest.async(block)(FakeRequest())).header.status shouldBe UNAUTHORIZED
      }
    }
  }

  ".individualAuthentication" should {
    "perform the block action" when {
      "the correct enrolment exist and nino exist" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtditid)), "Activated"),
          Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, mtditid)), "Activated")
        ))
        lazy val result: Result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))

          await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
        }

        "returns an OK status" in {
          result.header.status shouldBe OK
        }

        "returns a body of the mtditid" in {
          await(result.body.consumeData.map(_.utf8String)) shouldBe mtditid
        }
      }

      "the correct enrolment and nino exist but the request is for a different id" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, "123456")), "Activated"),
          Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, mtditid)), "Activated")
        ))
        lazy val result: Result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))

          await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
        }

        "returns an UNAUTHORIZED status" in {
          result.header.status shouldBe UNAUTHORIZED
        }
      }

      "the correct enrolment and nino exist but low CL" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtditid)), "Activated"),
          Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, mtditid)), "Activated")
        ))
        lazy val result: Result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))

          await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
        }

        "returns an UNAUTHORIZED status" in {
          result.header.status shouldBe UNAUTHORIZED
        }
      }

      "the correct enrolment exist but no nino" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtditid)), "Activated")))
        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))

          await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
        }

        "returns an 401 status" in {
          result.header.status shouldBe UNAUTHORIZED
        }
      }

      "the correct nino exist but no enrolment" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val id = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, id)), "Activated")))
        lazy val result: Result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))

          await(underTest.individualAuthentication(block, id)(requestWithMtditid, emptyHeaderCarrier))
        }

        "returns an 401 status" in {
          result.header.status shouldBe UNAUTHORIZED
        }
      }
    }

    "return a UNAUTHORIZED" when {
      "the correct enrolment is missing" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment("notAnIndividualOops", Seq(EnrolmentIdentifier(Individual.value, mtditid)), "Activated")))
        lazy val result: Result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))

          await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
        }

        "returns a forbidden" in {
          result.header.status shouldBe UNAUTHORIZED
        }
      }
    }

    "the correct enrolment and nino exist but the request is for a different id" which {
      val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(
        Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, "123456")), "Activated"),
        Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, mtditid)), "Activated")
      ))
      lazy val result: Result = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))

        await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
      }

      "returns an UNAUTHORIZED status" in {
        result.header.status shouldBe UNAUTHORIZED
      }
    }

    "the correct enrolment and nino exist but low CL" which {
      val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(
        Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtditid)), "Activated"),
        Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, mtditid)), "Activated")
      ))
      lazy val result: Result = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L50))

        await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
      }

      "returns an UNAUTHORIZED status" in {
        result.header.status shouldBe UNAUTHORIZED
      }
    }

    "the correct enrolment exist but no nino" which {
      val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtditid)), "Activated")))
      lazy val result: Result = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))

        await(underTest.individualAuthentication(block, mtditid)(requestWithMtditid, emptyHeaderCarrier))
      }

      "returns an 401 status" in {
        result.header.status shouldBe UNAUTHORIZED
      }
    }

    "the correct nino exist but no enrolment" which {
      val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
      val id = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(Nino.key, Seq(EnrolmentIdentifier(Nino.value, id)), "Activated")))
      lazy val result: Result = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))

        await(underTest.individualAuthentication(block, id)(requestWithMtditid, emptyHeaderCarrier))
      }

      "returns an 401 status" in {
        result.header.status shouldBe UNAUTHORIZED
      }
    }
  }

  ".agentAuthentication" when {
    "a valid request is made" which {
      "results in a NoActiveSession error to be returned from Auth" should {
        "return an Unauthorised response" in new AgentTest {
          object AuthException extends NoActiveSession("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }
      }

      "[EMA disabled] results in an AuthorisationException error being returned from Auth" should {
        "return an Unauthorised response" in new AgentTest {
          mockMultipleAgentsSwitch(false)

          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }
      }

      "[EMA enabled] results in an AuthorisationException error being returned from Auth" should {
        "return an Unauthorised response when secondary agent auth call also fails" in new AgentTest {
          mockMultipleAgentsSwitch(true)

          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturnException(AuthException, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }

        "handle appropriately when a supporting agent is properly authorised" in new AgentTest {
          mockMultipleAgentsSwitch(true)

          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturn(supportingAgentEnrolment, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe s"$mtdItId $arn"
        }
      }

      "results in successful authorisation for a primary agent" should {
        "return an Unauthorised response when an ARN cannot be found" in new AgentTest {
          val primaryAgentEnrolmentNoArn: Enrolments = Enrolments(Set(
            Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, mtdItId)), "Activated"),
            Enrolment(Agent.key, Seq.empty, "Activated")
          ))

          mockAuthReturn(primaryAgentEnrolmentNoArn, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe UNAUTHORIZED
          contentAsString(result) shouldBe ""
        }

        "invoke block when the user is properly authenticated" in new AgentTest {
          mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock, mtdItId)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe OK
          contentAsString(result) shouldBe s"$mtdItId $arn"
        }
      }
    }
  }

  ".enrolmentGetIdentifierValue" should {
    "return the value for a given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"
      val enrolments = Enrolments(Set(
        Enrolment(Individual.key, Seq(EnrolmentIdentifier(Individual.value, returnValue)), "Activated"),
        Enrolment(Agent.key, Seq(EnrolmentIdentifier(Agent.value, returnValueAgent)), "Activated")
      ))

      underTest.enrolmentGetIdentifierValue(Individual.key, Individual.value, enrolments) shouldBe Some(returnValue)
      underTest.enrolmentGetIdentifierValue(Agent.key, Agent.value, enrolments) shouldBe Some(returnValueAgent)
    }

    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"
      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))

      "the given identifier cannot be found" in {
        underTest.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) shouldBe None
      }

      "the given key cannot be found" in {
        underTest.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) shouldBe None
      }
    }
  }
}
