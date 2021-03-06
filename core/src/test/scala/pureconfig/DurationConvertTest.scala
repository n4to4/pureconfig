package pureconfig

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigValueFactory
import org.scalatest.{ FlatSpec, Matchers, TryValues }
import org.scalatest.Inspectors._

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

import scala.util.{ Failure, Success }

class DurationConvertTest extends FlatSpec with Matchers with TryValues {
  import DurationConvert.{ fromDuration => fromD }
  "Converting a Duration to a String" should "pick an appropriate unit when dealing with whole units less than the next step up" in {
    fromD(Duration(14, TimeUnit.DAYS)) shouldBe "14d"
    fromD(Duration(16, TimeUnit.HOURS)) shouldBe "16h"
    fromD(Duration(1, TimeUnit.MINUTES)) shouldBe "1m"
    fromD(Duration(33, TimeUnit.SECONDS)) shouldBe "33s"
    fromD(Duration(555, TimeUnit.MILLISECONDS)) shouldBe "555ms"
    fromD(Duration(999, TimeUnit.MICROSECONDS)) shouldBe "999us"
    fromD(Duration(222, TimeUnit.NANOSECONDS)) shouldBe "222ns"
  }
  it should "pick an appropriate unit when dealing with whole units more than, but fractional in, the next step up" in {
    fromD(Duration(1, TimeUnit.DAYS)) shouldBe "1d"
    fromD(Duration(47, TimeUnit.HOURS)) shouldBe "47h"
    fromD(Duration(123, TimeUnit.MINUTES)) shouldBe "123m"
    fromD(Duration(489, TimeUnit.SECONDS)) shouldBe "489s"
    fromD(Duration(11555, TimeUnit.MILLISECONDS)) shouldBe "11555ms"
    fromD(Duration(44999, TimeUnit.MICROSECONDS)) shouldBe "44999us"
    fromD(Duration(88222, TimeUnit.NANOSECONDS)) shouldBe "88222ns"
  }
  it should "write 0 without units" in {
    fromD(Duration(0, TimeUnit.DAYS)) shouldBe "0"
  }
  it should "handle an infinite duration" in {
    fromD(Duration.Inf) shouldBe "Inf"
  }
  it should "round trip negative infinity" in {
    val expected = Duration.MinusInf
    fromS(fromD(expected)).success.value shouldBe expected
  }

  val fromS = DurationConvert.fromString(_: String, implicitly[ClassTag[Duration]])
  "Converting a String to a Duration" should "succeed for known units" in {
    fromS("1d") shouldBe Success(Duration(1, TimeUnit.DAYS))
    fromS("47h") shouldBe Success(Duration(47, TimeUnit.HOURS))
    fromS("123m") shouldBe Success(Duration(123, TimeUnit.MINUTES))
    fromS("489s") shouldBe Success(Duration(489, TimeUnit.SECONDS))
    fromS("11555ms") shouldBe Success(Duration(11555, TimeUnit.MILLISECONDS))
    fromS("44999us") shouldBe Success(Duration(44999, TimeUnit.MICROSECONDS))
    fromS("44999µs") shouldBe Success(Duration(44999, TimeUnit.MICROSECONDS))
    fromS("88222ns") shouldBe Success(Duration(88222, TimeUnit.NANOSECONDS))
  }
  it should "succeed when loading 0 without units" in {
    val signs = Set("", "-", "+")
    val leftSpacing = Set("", " ")
    val rightSpacing = Set("", " ")
    val leftSpacingSize = Set((0 to 2): _*)
    val rightSpacingSize = Set((0 to 2): _*)
    val zeroRepeatSize = Set((1 to 2): _*)
    forAll(signs) { sign =>
      forAll(leftSpacing) { lsc =>
        forAll(rightSpacing) { rsc =>
          forAll(leftSpacingSize) { ls =>
            forAll(rightSpacingSize) { rs =>
              forAll(zeroRepeatSize) { zr =>
                fromS(lsc * ls + sign + "0" * zr + rsc * rs) shouldBe Success(Duration(0, TimeUnit.DAYS))
              }
            }
          }
        }
      }
    }
  }
  it should "report a helpful error message when failing to convert a bad duration" in {
    val badDuration = "10 lordsALeaping"
    val result = LocalDurationConvert.durationConfigConvert.from(ConfigValueFactory.fromAnyRef(badDuration))
    result match {
      case Success(_) => fail("Should be failure")
      case Failure(ex) =>
        val message = ex.getMessage
        println(message)
        message should include(badDuration)
    }
  }
  private object LocalDurationConvert extends LowPriorityConfigConvertImplicits
}
