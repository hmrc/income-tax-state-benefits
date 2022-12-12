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

package utils

import models.encryption.TextAndKey

import java.time.{Instant, LocalDate}
import java.util.UUID

trait Cypher[A] {
  self =>

  def encrypt(value: A)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue

  def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A

  def imap[B](dec: A => B, enc: B => A): Cypher[B] = new Cypher[B] {
    override def encrypt(value: B)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue =
      self.encrypt(enc(value))

    override def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): B =
      dec(self.decrypt(encryptedValue))
  }
}

object Cypher {

  implicit val stringCypher: Cypher[String] = new Cypher[String] {
    override def encrypt(value: String)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = secureGCMCipher.encrypt(value)

    override def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): String =
      secureGCMCipher.decrypt(encryptedValue.value, encryptedValue.nonce)
  }

  implicit val booleanCypher: Cypher[Boolean] = stringCypher.imap(_.toBoolean, _.toString)

  implicit val bigDecimalCypher: Cypher[BigDecimal] = stringCypher.imap(BigDecimal(_), _.toString)

  implicit val uuidCypher: Cypher[UUID] = stringCypher.imap(UUID.fromString, _.toString)

  implicit val localDateCypher: Cypher[LocalDate] = stringCypher.imap(LocalDate.parse, _.toString)

  implicit val instantCypher: Cypher[Instant] = stringCypher.imap(Instant.parse, _.toString)
}

object CypherSyntax {
  implicit class EncryptableOps[A](value: A)(implicit cypher: Cypher[A]) {
    def encrypted(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = cypher.encrypt(value)
  }

  implicit class DecryptableOps(encryptedValue: EncryptedValue) {
    def decrypted[A](implicit cypher: Cypher[A], secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A = cypher.decrypt(encryptedValue)
  }
}
