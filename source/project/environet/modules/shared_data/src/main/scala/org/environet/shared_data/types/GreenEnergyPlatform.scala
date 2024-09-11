package org.environet.shared_data.types

import cats.Eq
import cats.syntax.all._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.schema.balance.Amount
import org.tessellation.schema.epoch.EpochProgress

object GreenEnergyPlatform {
  @derive(encoder, decoder)
  case class GreenEnergyPlatformDataSource(
    epochProgressToReward: EpochProgress,
    amountToReward: Amount,
    latestProjects: Set[String],
    olderProjects: Set[String]
  )

  case class ProjectInfo(
    projectId: String,
    projectName: String,
  )

  case class GreenEnergyProject(
    id: String,
    energySaved: Double,
    projectInfo: ProjectInfo,
    status: String,
    startDate: String,
    location: String
  ) {
    implicit val eqInstance: Eq[GreenEnergyProject] = Eq.instance { (a, b) =>
      a.id === b.id
    }
  }

  case class GreenEnergyApiResponse(data: List[GreenEnergyProject])
}
