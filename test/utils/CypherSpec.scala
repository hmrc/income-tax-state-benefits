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

import models.encryption.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
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

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  "stringCypher" should {
    val stringValue = "some-string-value"
    "encrypt string values" in {
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(stringValue, textAndKey).returning(encryptedString)

      stringCypher.encrypt(stringValue) shouldBe encryptedString
    }

    "decrypt to string values" in {
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey).returning(stringValue)

      stringCypher.decrypt(encryptedValue) shouldBe stringValue
    }
  }

  "booleanCypher" should {
    val someBoolean = true
    "encrypt boolean values" in {
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(someBoolean.toString, textAndKey).returning(encryptedBoolean)

      booleanCypher.encrypt(someBoolean) shouldBe encryptedBoolean
    }

    "decrypt to boolean values" in {
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey).returning(someBoolean.toString)

      booleanCypher.decrypt(encryptedValue) shouldBe someBoolean
    }
  }

  "bigDecimalCypher" should {
    val bigDecimalValue: BigDecimal = 500.0
    "encrypt BigDecimal values" in {
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(bigDecimalValue.toString, textAndKey).returning(encryptedBigDecimal)

      bigDecimalCypher.encrypt(bigDecimalValue) shouldBe encryptedBigDecimal
    }

    "decrypt to BigDecimal values" in {
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey).returning(bigDecimalValue.toString)

      bigDecimalCypher.decrypt(encryptedValue) shouldBe bigDecimalValue
    }
  }

  "uuidCypher" should {
    val uuidValue: UUID = UUID.randomUUID()
    "encrypt UUID values" in {
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(uuidValue.toString, textAndKey).returning(encryptedUUID)

      uuidCypher.encrypt(uuidValue) shouldBe encryptedUUID
    }

    "decrypt to UUID values" in {
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey).returning(uuidValue.toString)

      uuidCypher.decrypt(encryptedValue) shouldBe uuidValue
    }
  }

  "localDateCypher" should {
    val localDateValue: LocalDate = LocalDate.now()
    "encrypt LocalDate values" in {
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(localDateValue.toString, textAndKey).returning(encryptedLocalDate)

      localDateCypher.encrypt(localDateValue) shouldBe encryptedLocalDate
    }

    "decrypt to LocalDate values" in {
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey).returning(localDateValue.toString)

      localDateCypher.decrypt(encryptedValue) shouldBe localDateValue
    }
  }

  "instantCypher" should {
    val instantValue: Instant = Instant.now()
    "encrypt Instance values" in {
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(instantValue.toString, textAndKey).returning(encryptedInstant)

      instantCypher.encrypt(instantValue) shouldBe encryptedInstant
    }

    "decrypt to LocalDate values" in {
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey).returning(instantValue.toString)

      instantCypher.decrypt(encryptedValue) shouldBe instantValue
    }
  }
}
