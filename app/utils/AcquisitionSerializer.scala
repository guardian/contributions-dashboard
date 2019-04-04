package utils

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.Decoder
import ophan.thrift.event.Acquisition
import com.gu.fezziwig.CirceScroogeMacros._

object AcquisitionSerializer {
  private implicit val decoder = Decoder[Acquisition]

  def apply(acquisition: Acquisition): String = acquisition.asJson.noSpaces
}
