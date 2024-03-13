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

package unit.logger

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.UnitSpec

class PassByNameVerifierSpec extends UnitSpec with MockitoSugar with Matchers {

  private trait LoggerToMock {
    def error(msg: => String, e: => Throwable): Unit
    def error(msg: => String)(implicit hc: HeaderCarrier): Unit
  }

  private trait SetUp {
    val mockLogger: LoggerToMock = mock[LoggerToMock]
    val expectedException: RuntimeException = new RuntimeException("expectedException")
    val notExpectedException: RuntimeException = new RuntimeException("notExpectedException")
  }

  "PassByNameVerifier" can {
    "In a scenario where we want to match by name parameters using an EQ matcher" should {
      "verify String and Throwable pass by name parameters" in new SetUp {
        mockLogger.error("ERROR", expectedException)

        PassByNameVerifier(mockLogger, "error")
          .withByNameParam[String]("ERROR")
          .withByNameParam[Throwable](expectedException)
          .verify()
      }

      "verify String pass by name parameters and implicit Header Carrier" in new SetUp {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        mockLogger.error("ERROR")

        PassByNameVerifier(mockLogger, "error")
          .withByNameParam[String]("ERROR")
          .withParamMatcher(any[HeaderCarrier])
          .verify()
      }
    }

    "In a scenario where we have both normal and by name parameters, PassByNameVerifier" should {
      class SomeClass
      class Foo {
        def fooWithClass(someClass: SomeClass, f: => Unit): Int = 1
        def fooWithInt(someValue: Int, f: => Unit): Int = 1
        def fooWithString(someValue: Int, f: => String): Int = 1
      }

      "verify ArgumentMatchers for a class and by name param" in {
        val mockFoo = mock[Foo]
        val someClass = new SomeClass

        mockFoo.fooWithClass(someClass, ())

        PassByNameVerifier(mockFoo, "fooWithClass")
          .withParamMatcher(ameq(someClass))
          .withByNameParamMatcher(any[() => Unit])
          .verify()
      }

      "verify ArgumentMatchers for a primitive and any by name param" in {
        val mockFoo = mock[Foo]

        mockFoo.fooWithString(1, "X")

        PassByNameVerifier(mockFoo, "fooWithString")
          .withParamMatcher(ameq(1))
          .withByNameParamMatcher(any[() => Unit])
          .verify()
      }

      "verify ArgumentMatchers for a primitive and an EQ by name param" in {
        val mockFoo = mock[Foo]

        mockFoo.fooWithString(1, "X")

        PassByNameVerifier(mockFoo, "fooWithString")
          .withParamMatcher(ameq(1))
          .withByNameParamMatcher(ArgumentMatchers.argThat(byNameEqArgMatcher(() => "X")))
          .verify()
      }

      "verify ArgumentMatchers a primitive and an EQ by name param using withByNameParamMatch" in {
        val mockFoo = mock[Foo]

        mockFoo.fooWithString(1, "X")

        PassByNameVerifier(mockFoo, "fooWithString")
          .withParamMatcher(ameq(1))
          .withByNameParamMatcher(ArgumentMatchers.argThat(byNameEqArgMatcher(() => "X")))
          .verify()
      }

      "verify ArgumentMatchers for a primitive and an EQ by name param using withByNameParam" in {
        val mockFoo = mock[Foo]

        mockFoo.fooWithString(1, "X")

        PassByNameVerifier(mockFoo, "fooWithString")
          .withParamMatcher(ameq(1))
          .withByNameParam("X")
          .verify()
      }
    }

    "in un-happy path" should {
      "verify there were zero interactions with this mock" in new SetUp {
        private val passByName = PassByNameVerifier(mockLogger, "error")
        .withByNameParam[String]("ERROR")
        .withByNameParam[Throwable](expectedException)

        private val caught = intercept[Throwable](passByName.verify())

        caught.getCause.getMessage should include("there were zero interactions with this mock")
      }

      "verify there IllegalArgumentException thrown when verifying with empty parameters" in new SetUp {
        private val passByName = PassByNameVerifier(mockLogger, "error")
        mockLogger.error("ERROR", expectedException)

        private val caught = intercept[Throwable](passByName.verify())

        caught.getMessage should include("no parameters specified.")
      }

      "verify wrong parameter type specification" in new SetUp {
        private val wrongParamType: Int = 1
        mockLogger.error("ERROR", expectedException)
        private val passByName = PassByNameVerifier(mockLogger, "error")
          .withByNameParam[Int](wrongParamType)
          .withByNameParam[Throwable](expectedException)

        private val caught = intercept[Throwable](passByName.verify())

        verifyInternalError(caught, "Argument(s) are different! Wanted:")
      }

      "verify String parameter value matching errors" in new SetUp {
        mockLogger.error("VALUE_DOES_NOT_MATCH", expectedException)
        private val passByName = PassByNameVerifier(mockLogger, "error")
          .withByNameParam[String]("ERROR")
          .withByNameParam[Throwable](expectedException)

        private val caught = intercept[Throwable](passByName.verify())

        verifyInternalError(caught, "Argument(s) are different! Wanted:")
      }

      "verify Throwable parameter value matching errors" in new SetUp {
        mockLogger.error("Error", notExpectedException)
        private val passByName = PassByNameVerifier(mockLogger, "error")
          .withByNameParam[String]("ERROR")
          .withByNameParam[Throwable](expectedException)

        private val caught = intercept[Throwable](passByName.verify())

        verifyInternalError(caught, "Argument(s) are different! Wanted:")
      }
    }

  }

  private def byNameEqArgMatcher[P](compare: P) = new ArgumentMatcher[P] {
    override def matches(argument: P): Boolean =
      argument.asInstanceOf[() => P].apply() == compare.asInstanceOf[() => P].apply()
  }

  private def verifyInternalError(caught: Throwable, msg: String) = {
    caught.getCause.getMessage should include(msg)
  }
}
