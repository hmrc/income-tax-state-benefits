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

import sbt.*

object AppDependencies {

  private val bootstrapBackendPlay30Version = "8.4.0"
  private val hmrcMongoPlay30Version = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapBackendPlay30Version,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % hmrcMongoPlay30Version,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.14.2",
    "org.typelevel"                 %% "cats-core"                  % "2.9.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapBackendPlay30Version % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoPlay30Version        % Test,
    "org.scalamock"           %% "scalamock"                  % "5.2.0"                       % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.15"                      % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.0"                      % Test,
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.35.0"                      % Test
  )
}
