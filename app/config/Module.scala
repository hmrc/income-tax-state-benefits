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

package config

import play.api.inject.Binding
import play.api.{Configuration, Environment}
import repositories.{JourneyAnswersRepository, JourneyAnswersRepositoryImpl, StateBenefitsUserDataRepository, StateBenefitsUserDataRepositoryImpl}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import utils.{AesGcmCryptoProvider, StartUpLogging}

import java.time.Clock

class Module extends play.api.inject.Module {
  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    Seq(
      bind[Encrypter with Decrypter].toProvider[AesGcmCryptoProvider],
      bind[AppConfig].to(classOf[AppConfigImpl]),
      bind[Clock].toInstance(Clock.systemUTC()),
      bind[StateBenefitsUserDataRepository].to[StateBenefitsUserDataRepositoryImpl].eagerly(),
      bind[JourneyAnswersRepository].to[JourneyAnswersRepositoryImpl].eagerly(),
      bind[StartUpLogging].toSelf.eagerly()
    )
  }
}
