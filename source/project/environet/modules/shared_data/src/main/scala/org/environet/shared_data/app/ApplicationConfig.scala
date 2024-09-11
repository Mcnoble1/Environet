package org.environet_metagraph.shared_data.app

import ciris.Secret
import fs2.io.file.Path
import org.environet_metagraph.shared_data.types.Refined.ApiUrl
import org.tessellation.node.shared.config.types.HttpClientConfig
import org.tessellation.schema.address.Address
import org.tessellation.schema.balance.Amount

import java.time.LocalDate
import scala.concurrent.duration._

case class ApplicationConfig(
  http4s                          : ApplicationConfig.Http4sConfig,
  greenEnergyPlatformsDaemon      : ApplicationConfig.GreenEnergyPlatformsDaemonConfig,
  carbonFootprintCalculatorsDaemon: ApplicationConfig.CarbonFootprintCalculatorsDaemonConfig,
  investmentPlatformsDaemon       : ApplicationConfig.InvestmentPlatformsDaemonConfig,
  manualSubmissionDaemon          : ApplicationConfig.ManualSubmissionDaemonConfig,
  walletCreationHoldingDagDaemon  : ApplicationConfig.WalletCreationHoldingDagDaemonConfig,
  nodeKey                         : ApplicationConfig.NodeKey
)

object ApplicationConfig {

  case class Http4sConfig(
    client: HttpClientConfig,
  )

  // Green Energy Platforms Daemon Config
  case class GreenEnergyPlatformsDaemonConfig(
    idleTime: FiniteDuration,
    apiKey  : Option[String],
    apiUrl  : Option[ApiUrl]
  )

  // Carbon Footprint Calculators Daemon Config
  case class CarbonFootprintCalculatorsDaemonConfig(
    idleTime: FiniteDuration,
    apiKey  : Option[String],
    apiUrl  : Option[ApiUrl]
  )

  // Investment Platforms Daemon Config
  case class InvestmentPlatformsDaemonConfig(
    idleTime: FiniteDuration,
    apiKey  : Option[String],
    apiUrl  : Option[ApiUrl]
  )

  // Manual Submission Daemon Config (for manually submitted green energy data)
  case class ManualSubmissionDaemonConfig(
    idleTime: FiniteDuration,
    apiUrl  : Option[ApiUrl]
  )

  // Wallet Creation and Holding DAG Daemon Config
  case class WalletCreationHoldingDagDaemonConfig(
    idleTime: FiniteDuration,
    apiUrl  : Option[ApiUrl]
  )

  // Node Key Configuration
  case class NodeKey(
    keystore: Path,
    alias   : Secret[String],
    password: Secret[String]
  )
}
