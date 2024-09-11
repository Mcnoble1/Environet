package org.environet.l0.custom_routes

import cats.effect.Async
import cats.syntax.all._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.auto._
import org.environet.shared_data.calculated_state.CalculatedStateService
import org.environet.shared_data.types.States.DataSourceType._
import org.environet.shared_data.types.States.EnvironetCalculatedState
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.http4s.{HttpRoutes, Response}
import org.tessellation.routes.internal.{InternalUrlPrefix, PublicRoutes}

case class CustomRoutes[F[_] : Async](calculatedStateService: CalculatedStateService[F]) extends Http4sDsl[F] with PublicRoutes[F] {

  @derive(encoder, decoder)
  case class CalculatedStateResponse(
    ordinal        : Long,
    calculatedState: EnvironetCalculatedState
  )

  private def getLatestCalculatedState: F[Response[F]] = {
    calculatedStateService
      .get
      .flatMap(state => Ok(CalculatedStateResponse(state.ordinal.value.value, state.state)))
  }

  private def getCalculatedStateByDataSource(dataSourceName: String): F[Response[F]] = {
    val dataSourceMap = Map(
      "solaredge" -> GreenEnergyPlatform,
      "tesla" -> GreenEnergyPlatform,
      "rec" -> GreenEnergyPlatform,
      "smartmeter" -> GreenEnergyPlatform,
      "carbonfootprint" -> CarbonFootprintCalculator,
      "investment" -> InvestmentPlatform,
      "manualsubmission" -> ManualSubmission
    )

    calculatedStateService
      .get
      .flatMap { state =>
        dataSourceMap
          .get(dataSourceName.toLowerCase)
          .map(state.state.dataSources.get)
          .map(Ok(_))
          .getOrElse(NotFound())
      }
  }

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "calculated-state" => getLatestCalculatedState
    case GET -> Root / "calculated-state" / dataSourceName => getCalculatedStateByDataSource(dataSourceName)
  }

  val public: HttpRoutes[F] =
    CORS
      .policy
      .withAllowCredentials(false)
      .httpRoutes(routes)

  override protected def prefixPath: InternalUrlPrefix = "/"
}
