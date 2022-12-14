/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import connectors.errors.{ApiError, SingleErrorBody}
import models.errors.ApiServiceError
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.UnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.MockSubmissionConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionServiceSpec extends UnitTest
  with MockSubmissionConnector {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val anyTaxYear = 2022
  private val anyNino = "any-nino"
  private val mtdItId = aStateBenefitsUserData.mtdItId
  private val apiError = ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason"))

  private val underTest = new SubmissionService(mockSubmissionConnector)

  ".getIncomeTaxUserData" should {
    "return error when connector.getIncomeTaxUserData(...) fails" in {
      mockGetIncomeTaxUserData(anyTaxYear, anyNino, mtdItId, Left(apiError))

      await(underTest.getIncomeTaxUserData(anyTaxYear, anyNino, mtdItId)) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "return success when connector.getIncomeTaxUserData(...) succeeds" in {
      mockGetIncomeTaxUserData(anyTaxYear, anyNino, mtdItId, Right(anIncomeTaxUserData))

      await(underTest.getIncomeTaxUserData(anyTaxYear, anyNino, mtdItId)) shouldBe Right(anIncomeTaxUserData)
    }
  }

  ".refreshStateBenefits" should {
    "return error when connector.refreshStateBenefits(...) fails" in {
      mockRefreshStateBenefits(anyTaxYear, anyNino, mtdItId, Left(apiError))

      await(underTest.refreshStateBenefits(anyTaxYear, anyNino, mtdItId)) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "return success when connector.refreshStateBenefits(...) succeeds" in {
      mockRefreshStateBenefits(anyTaxYear, anyNino, mtdItId, Right(()))

      await(underTest.refreshStateBenefits(anyTaxYear, anyNino, mtdItId)) shouldBe Right(())
    }
  }
}
