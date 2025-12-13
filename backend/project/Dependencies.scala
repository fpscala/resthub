import sbt.*

object Dependencies {
  object Versions {
    lazy val circe = "0.14.3"
    lazy val doobie = "1.0.0-RC5"
    lazy val http4s = "0.23.10"
    lazy val flyway = "11.3.1"
    lazy val refined = "0.10.3"
    lazy val cats = "2.13.0"
    lazy val `cats-retry` = "3.1.3"
    lazy val `cats-effect` = "3.5.7"
    lazy val logback = "1.5.16"
    lazy val log4cats = "2.7.0"
    lazy val pureconfig = "0.17.8"
    lazy val fs2 = "3.9.3"
    lazy val enumeratum = "1.7.5"
    lazy val sttp = "3.7.2"
    lazy val `http4s-jwt-auth` = "2.0.2"
    lazy val newtype = "0.4.4"
    lazy val tsec = "0.5.0"
    lazy val monocle = "3.3.0"
    lazy val redis4cats = "1.1.1"
    lazy val `cats-tagless` = "0.16.3"
    lazy val derevo = "0.13.0"
    lazy val postgresql = "42.7.5"
    lazy val awsSdk = "1.12.583"
    lazy val awsSoftwareS3 = "2.30.16"
    lazy val weaver = "0.8.4"
    lazy val chimney = "1.7.3"
    lazy val `test-container` = "1.20.4"
    lazy val cron4s = "0.7.0"
    lazy val `fs2-cron4s` = "0.9.0"
    lazy val uzscala = "1.0.18"
    lazy val mailer = "1.4.7"
    lazy val `fs2-kafka` = "3.0.1"
    lazy val telegramium = "10.902.0"
  }
  trait LibGroup {
    def all: Seq[ModuleID]
  }
  object com {
    object github {
      object cb372 {
        lazy val retry = "com.github.cb372" %% "cats-retry" % Versions.`cats-retry`
      }
      object fd4s {
        lazy val `fs2-kafka` = "com.github.fd4s" %% "fs2-kafka" % Versions.`fs2-kafka`
      }
      object pureconfig extends LibGroup {
        private def repo(artifact: String): ModuleID =
          "com.github.pureconfig" %% artifact % Versions.pureconfig
        lazy val core: ModuleID = repo("pureconfig")
        lazy val enumeratum: ModuleID = repo("pureconfig-enumeratum")
        lazy val cron4s: ModuleID = repo("pureconfig-cron4s")

        override def all: Seq[ModuleID] = Seq(core, enumeratum, cron4s)
      }
      lazy val cron4s: ModuleID =
        "com.github.alonsodomin.cron4s" %% s"cron4s-core" % Versions.cron4s
    }
    object beachape {
      object enumeratum extends LibGroup {
        private def enumeratum(artifact: String): ModuleID =
          "com.beachape" %% artifact % Versions.enumeratum

        lazy val core: ModuleID = enumeratum("enumeratum")
        lazy val circe: ModuleID = enumeratum("enumeratum-circe")
        lazy val cats: ModuleID = enumeratum("enumeratum-cats")
        lazy val doobie: ModuleID = enumeratum("enumeratum-doobie")
        override def all: Seq[ModuleID] = Seq(core, circe, cats, doobie)
      }
    }
    object softwaremill {
      object sttp extends LibGroup {
        private def sttp(artifact: String): ModuleID =
          "com.softwaremill.sttp.client3" %% artifact % Versions.sttp

        lazy val circe: ModuleID = sttp("circe")
        lazy val `fs2-backend`: ModuleID = sttp("async-http-client-backend-fs2")
        override def all: Seq[ModuleID] = Seq(circe, `fs2-backend`)
      }
    }

    object amazonaws extends LibGroup {
      private def awsJdk(artifact: String): ModuleID =
        "com.amazonaws" % artifact % Versions.awsSdk

      lazy val awsCore: ModuleID = awsJdk("aws-java-sdk-core")
      lazy val awsS3: ModuleID = awsJdk("aws-java-sdk-s3")
      val awsSoftwareS3: ModuleID = "software.amazon.awssdk" % "s3" % Versions.awsSoftwareS3

      override def all: Seq[ModuleID] = Seq(awsCore, awsS3, awsSoftwareS3)
    }

    object disneystreaming extends LibGroup {
      private def weaver(artifact: String): ModuleID =
        "com.disneystreaming" %% s"weaver-$artifact" % Versions.weaver

      lazy val cats: ModuleID = weaver("cats")
      lazy val discipline: ModuleID = weaver("discipline")
      lazy val `scala-check`: ModuleID = weaver("scalacheck")

      override def all: Seq[ModuleID] = Seq(cats, discipline, `scala-check`)
    }
  }
  object io {
    object scalaland {
      lazy val chimney: ModuleID = "io.scalaland" %% "chimney" % Versions.chimney
    }
    object circe extends LibGroup {
      private def circe(artifact: String): ModuleID =
        "io.circe" %% s"circe-$artifact" % Versions.circe

      lazy val core: ModuleID = circe("core")
      lazy val generic: ModuleID = circe("generic")
      lazy val parser: ModuleID = circe("parser")
      lazy val refined: ModuleID = circe("refined")
      lazy val `generic-extras`: ModuleID = circe("generic-extras")
      override def all: Seq[ModuleID] = Seq(core, generic, parser, refined, `generic-extras`)
    }

    object estatico {
      lazy val newtype = "io.estatico" %% "newtype" % Versions.newtype
    }
    object github {
      object jmcardon {
        lazy val `tsec-password` = "io.github.jmcardon" %% "tsec-password" % Versions.tsec
      }
      object apimorphism {
        object telegramium extends LibGroup {
          private def telegramium(artifact: String): ModuleID =
            "io.github.apimorphism" %% artifact % Versions.telegramium

          lazy val core: ModuleID = telegramium("telegramium-core")
          lazy val high: ModuleID = telegramium("telegramium-high")

          override def all: Seq[ModuleID] = Seq(core, high)
        }
      }
    }
  }
  object org {
    lazy val postgresql: ModuleID = "org.postgresql" % "postgresql" % Versions.postgresql
    lazy val testcontainers: ModuleID =
      "org.testcontainers" % "postgresql" % Versions.`test-container`

    object typelevel {
      object cats {
        lazy val core = "org.typelevel"           %% "cats-core"           % Versions.cats
        lazy val effect = "org.typelevel"         %% "cats-effect"         % Versions.`cats-effect`
        lazy val `cats-tagless` = "org.typelevel" %% "cats-tagless-macros" % Versions.`cats-tagless`
      }
      lazy val log4cats = "org.typelevel" %% "log4cats-slf4j" % Versions.log4cats
    }

    object http4s extends LibGroup {
      private def http4s(artifact: String): ModuleID =
        "org.http4s" %% s"http4s-$artifact" % Versions.http4s

      lazy val dsl = http4s("dsl")
      lazy val server = http4s("ember-server")
      lazy val client = http4s("ember-client")
      lazy val circe = http4s("circe")
      lazy val `blaze-server` = http4s("blaze-server")
      override def all: Seq[ModuleID] = Seq(dsl, server, client, circe)
    }

    object flywaydb {
      lazy val core = "org.flywaydb"       % "flyway-core"                % Versions.flyway
      lazy val postgresql = "org.flywaydb" % "flyway-database-postgresql" % Versions.flyway
    }
  }
  object eu {
    object timepit {
      object refined extends LibGroup {
        private def refined(artifact: String): ModuleID =
          "eu.timepit" %% artifact % Versions.refined

        lazy val core = refined("refined")
        lazy val cats = refined("refined-cats")
        lazy val `refined-scalacheck` = refined("refined-scalacheck")
        lazy val pureconfig: ModuleID = refined("refined-pureconfig")

        override def all: Seq[ModuleID] = Seq(core, cats, pureconfig, `refined-scalacheck`)
      }

      lazy val cron4s: ModuleID = "eu.timepit" %% "fs2-cron-cron4s" % Versions.`fs2-cron4s`
    }
  }

  object ch {
    object qos {
      lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback
    }
  }

  object co {
    object fs2 extends LibGroup {
      private def fs2(artifact: String): ModuleID =
        "co.fs2" %% s"fs2-$artifact" % Versions.fs2

      lazy val core: ModuleID = fs2("core")
      lazy val io: ModuleID = fs2("io")
      override def all: Seq[ModuleID] = Seq(core, io)
    }
  }

  object tf {
    object tofu {
      object derevo extends LibGroup {
        private def derevo(artifact: String): ModuleID =
          "tf.tofu" %% s"derevo-$artifact" % Versions.derevo

        lazy val core: ModuleID = derevo("core")
        lazy val cats: ModuleID = derevo("cats")
        override def all: Seq[ModuleID] = Seq(core, cats)
      }
    }
  }
  object dev {
    object optics {
      lazy val monocle = "dev.optics" %% "monocle-core" % Versions.monocle
    }
    object profunktor {
      object redis4cats extends LibGroup {
        private def redis4cats(artifact: String): ModuleID =
          "dev.profunktor" %% artifact % Versions.redis4cats

        lazy val catsEffects: ModuleID = redis4cats("redis4cats-effects")
        lazy val log4cats: ModuleID = redis4cats("redis4cats-log4cats")
        override def all: Seq[ModuleID] = Seq(catsEffects, log4cats)
      }
      lazy val `http4s-jwt-auth` =
        "dev.profunktor" %% "http4s-jwt-auth" % Versions.`http4s-jwt-auth`
    }
  }
  object javax {
    lazy val mailer = "javax.mail" % "mail" % Versions.mailer
  }
  object uz {
    object scala extends LibGroup {
      lazy val common: ModuleID = "uz.scala" %% "common" % Versions.uzscala
      lazy val doobie: ModuleID = "uz.scala" %% "doobie" % Versions.uzscala
      lazy val flyway: ModuleID = "uz.scala" %% "flyway" % Versions.uzscala

      override def all: Seq[ModuleID] = Seq(common, doobie, flyway)
    }
  }
}
