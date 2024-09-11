package org.environet_metagraph.l0

import cats.effect.std.Supervisor
import cats.effect.{IO, Resource}
import cats.syntax.all._
import org.environet_metagraph.l0.rewards.EnvironetRewards
import org.environet_metagraph.shared_data.Utils.loadKeyPair
import org.environet_metagraph.shared_data.app.ApplicationConfigOps
import org.environet_metagraph.shared_data.calculated_state.CalculatedStateService
import org.environet_metagraph.shared_data.daemons.DaemonApis
import org.environet_metagraph.shared_data.types.codecs.JsonBinaryCodec
import org.environet_metagraph.schema.{EnvironetSnapshotEvent, EnvironetSnapshotStateProof}
import org.environet_metagraph.security.SecurityProvider
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.l0.CurrencyL0App
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.semver.{MetagraphVersion, TessellationVersion}
import org.tessellation.node.shared.domain.rewards.Rewards
import org.tessellation.node.shared.snapshot.currency.CurrencyIncrementalSnapshot

import java.util.UUID

object Main extends CurrencyL0App(
  "environet-l0",
  "Environet L0 node",
  ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
  tessellationVersion = TessellationVersion.unsafeFrom(org.tessellation.BuildInfo.version),
  metagraphVersion = MetagraphVersion.unsafeFrom(org.environet_metagraph.l0.BuildInfo.version)
) {

  override def dataApplication: Option[Resource[IO, BaseDataApplicationL0Service[IO]]] = (for {
    implicit0(supervisor: Supervisor[IO]) <- Supervisor[IO]
    implicit0(sp: SecurityProvider[IO]) <- SecurityProvider.forAsync[IO]
    implicit0(json2bin: JsonSerializer[IO]) <- JsonBinaryCodec.forSync[IO].asResource

    config <- ApplicationConfigOps.readDefault[IO].asResource
    keyPair <- loadKeyPair[IO](config).asResource
    calculatedStateService <- CalculatedStateService.make[IO].asResource
    _ <- DaemonApis.make[IO](config, keyPair).spawnL0Daemons(calculatedStateService).asResource
    l0Service <- MetagraphL0Service.make[IO](calculatedStateService, config).asResource
  } yield l0Service).some

  override def rewards(implicit sp: SecurityProvider[IO]): Option[Rewards[IO, EnvironetSnapshotStateProof, CurrencyIncrementalSnapshot, EnvironetSnapshotEvent]] =
    EnvironetRewards.make[IO]().some
}
