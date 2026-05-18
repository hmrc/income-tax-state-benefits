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

package support.mocks

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{when, withSettings}
import org.mockito.quality.Strictness
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthConnector extends MockitoSugar {

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector](withSettings().strictness(Strictness.STRICT_STUBS))

  def mockAuthReturnException(exception: Exception): Unit =
    when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[_]]())(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.failed(exception))

  def mockAuthAsAgent(enrolments: Enrolments): Unit = {
    when(mockAuthConnector.authorise(any[Predicate](), eqTo(Retrievals.affinityGroup))(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.successful(Some(AffinityGroup.Agent)))

    when(mockAuthConnector.authorise(any[Predicate](), eqTo(Retrievals.allEnrolments))(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.successful(enrolments))
  }

  def mockAuth(enrolments: Enrolments): Unit = {
    when(mockAuthConnector.authorise(any[Predicate](), eqTo(Retrievals.affinityGroup))(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.successful(Some(AffinityGroup.Individual)))

    when(mockAuthConnector.authorise(any[Predicate](), eqTo(Retrievals.allEnrolments and Retrievals.confidenceLevel))(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))
  }
}
