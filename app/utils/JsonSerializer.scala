package utils

import io.circe.generic.auto._
import io.circe.syntax._
import ophan.thrift.event.Acquisition
import com.gu.fezziwig.CirceScroogeMacros._
import models.RankIndexData

object JsonSerializer {

  def apply(acquisition: Acquisition): String = acquisition.asJson.noSpaces

  def apply(rankIndexData: List[RankIndexData]): String = rankIndexData.asJson.noSpaces
}
