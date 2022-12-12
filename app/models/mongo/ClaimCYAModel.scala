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

package models.mongo

import models.encryption.TextAndKey
import play.api.libs.json.{Json, OFormat}
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}
import utils.{EncryptedValue, SecureGCMCipher}

import java.time.{Instant, LocalDate}
import java.util.UUID

case class ClaimCYAModel(benefitId: Option[UUID] = None,
                         startDate: LocalDate,
                         endDateQuestion: Option[Boolean] = None,
                         endDate: Option[LocalDate] = None,
                         dateIgnored: Option[Instant] = None,
                         submittedOn: Option[Instant] = None,
                         amount: Option[BigDecimal] = None,
                         taxPaidQuestion: Option[Boolean] = None,
                         taxPaid: Option[BigDecimal] = None,
                         isHmrcData: Boolean) {

  def encrypted(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedClaimCYAModel = EncryptedClaimCYAModel(
    benefitId = benefitId.map(_.encrypted),
    startDate = startDate.encrypted,
    endDateQuestion = endDateQuestion.map(_.encrypted),
    endDate = endDate.map(_.encrypted),
    dateIgnored = dateIgnored.map(_.encrypted),
    submittedOn = submittedOn.map(_.encrypted),
    amount = amount.map(_.encrypted),
    taxPaidQuestion = taxPaidQuestion.map(_.encrypted),
    taxPaid = taxPaid.map(_.encrypted),
    isHmrcData = isHmrcData.encrypted
  )
}

object ClaimCYAModel {
  implicit val format: OFormat[ClaimCYAModel] = Json.format[ClaimCYAModel]
}

case class EncryptedClaimCYAModel(benefitId: Option[EncryptedValue],
                                  startDate: EncryptedValue,
                                  endDateQuestion: Option[EncryptedValue] = None,
                                  endDate: Option[EncryptedValue] = None,
                                  dateIgnored: Option[EncryptedValue] = None,
                                  submittedOn: Option[EncryptedValue] = None,
                                  amount: Option[EncryptedValue] = None,
                                  taxPaidQuestion: Option[EncryptedValue] = None,
                                  taxPaid: Option[EncryptedValue] = None,
                                  isHmrcData: EncryptedValue) {

  def decrypted(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): ClaimCYAModel = ClaimCYAModel(
    benefitId = benefitId.map(_.decrypted[UUID]),
    startDate = startDate.decrypted[LocalDate],
    endDateQuestion = endDateQuestion.map(_.decrypted[Boolean]),
    endDate = endDate.map(_.decrypted[LocalDate]),
    dateIgnored = dateIgnored.map(_.decrypted[Instant]),
    submittedOn = submittedOn.map(_.decrypted[Instant]),
    amount = amount.map(_.decrypted[BigDecimal]),
    taxPaidQuestion = taxPaidQuestion.map(_.decrypted[Boolean]),
    taxPaid = taxPaid.map(_.decrypted[BigDecimal]),
    isHmrcData = isHmrcData.decrypted[Boolean]
  )
}

object EncryptedClaimCYAModel {
  implicit val format: OFormat[EncryptedClaimCYAModel] = Json.format[EncryptedClaimCYAModel]
}
