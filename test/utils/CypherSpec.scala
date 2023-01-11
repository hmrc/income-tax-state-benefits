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

package utils

import org.scalamock.scalatest.MockFactory
import support.UnitTest
import uk.gov.hmrc.crypto.EncryptedValue
import utils.Cypher.{bigDecimalCypher, booleanCypher, instantCypher, localDateCypher, stringCypher, uuidCypher}

import java.time.{Instant, LocalDate}
import java.util.UUID

class CypherSpec extends UnitTest
  with MockFactory {

  private val encryptedBoolean = mock[EncryptedValue]
  private val encryptedString = mock[EncryptedValue]
  private val encryptedBigDecimal = mock[EncryptedValue]
  private val encryptedUUID = mock[EncryptedValue]
  private val encryptedLocalDate = mock[EncryptedValue]
  private val encryptedInstant = mock[EncryptedValue]
  private val encryptedValue = EncryptedValue("some-value", "some-nonce")

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  "stringCypher" should {
    val stringValue = "some-string-value"
    "encrypt string values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(stringValue, associatedText).returning(encryptedString)

      stringCypher.encrypt(stringValue) shouldBe encryptedString
    }

    "decrypt to string values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(stringValue)

      stringCypher.decrypt(encryptedValue) shouldBe stringValue
    }
  }

  "booleanCypher" should {
    val someBoolean = true
    "encrypt boolean values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(someBoolean.toString, associatedText).returning(encryptedBoolean)

      booleanCypher.encrypt(someBoolean) shouldBe encryptedBoolean
    }

    "decrypt to boolean values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(someBoolean.toString)

      booleanCypher.decrypt(encryptedValue) shouldBe someBoolean
    }
  }

  "bigDecimalCypher" should {
    val bigDecimalValue: BigDecimal = 500.0
    "encrypt BigDecimal values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(bigDecimalValue.toString, associatedText).returning(encryptedBigDecimal)

      bigDecimalCypher.encrypt(bigDecimalValue) shouldBe encryptedBigDecimal
    }

    "decrypt to BigDecimal values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(bigDecimalValue.toString)

      bigDecimalCypher.decrypt(encryptedValue) shouldBe bigDecimalValue
    }
  }

  "uuidCypher" should {
    val uuidValue: UUID = UUID.randomUUID()
    "encrypt UUID values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(uuidValue.toString, associatedText).returning(encryptedUUID)

      uuidCypher.encrypt(uuidValue) shouldBe encryptedUUID
    }

    "decrypt to UUID values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(uuidValue.toString)

      uuidCypher.decrypt(encryptedValue) shouldBe uuidValue
    }
  }

  "localDateCypher" should {
    val localDateValue: LocalDate = LocalDate.now()
    "encrypt LocalDate values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(localDateValue.toString, associatedText).returning(encryptedLocalDate)

      localDateCypher.encrypt(localDateValue) shouldBe encryptedLocalDate
    }

    "decrypt to LocalDate values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(localDateValue.toString)

      localDateCypher.decrypt(encryptedValue) shouldBe localDateValue
    }
  }

  "instantCypher" should {
    val instantValue: Instant = Instant.now()
    "encrypt Instance values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(instantValue.toString, associatedText).returning(encryptedInstant)

      instantCypher.encrypt(instantValue) shouldBe encryptedInstant
    }

    "decrypt to LocalDate values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(instantValue.toString)

      instantCypher.decrypt(encryptedValue) shouldBe instantValue
    }
  }
}
