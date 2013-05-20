package controllers

import anorm.SqlParser._
import anorm._
import anorm.~
import play.api._
import play.api.db.DB
import play.api.mvc._
import play.api.libs.json._

import scalaz._
import scalaz.std.list.listMonoid
import scala.util.{Try, Failure, Success}


object Root extends Controller {
  import play.api.Play.current

  case class Skill(name: String, description: String)
  case class UserSkillPicker(id: String, name: String, enabled: Boolean)
  private implicit val skillPayloadFmt = Json.format[UserSkillPicker]


  // a value of generic type A, along with out of band logger that accumulates List[String]
  type MyLogger[+A] = WriterT[scalaz.Id.Id, List[String], A]

  // provide fake monoid to make it compile... i dont think it gets used
  implicit object monoid extends Monoid[Throwable] {
    def zero: Throwable = null
    def append(v1: Throwable, v2: => Throwable): Throwable = ???
  }


  // play http entry point for a JSON resource - responds to GET /api/list-skills-user-picker
  // performs two queries, parses them into a single payload, renders http response
  def index = Action { request =>

    val x: EitherT[MyLogger, Throwable, List[UserSkillPicker]] = DB.withConnection { dbconn =>

      val a: EitherT[MyLogger, Throwable, List[UserSkillPicker]] =

        for {
          allSkills: Map[String, Skill] <- SkillsMapping.all(dbconn)             // query can fail
          userSkills: Map[String, Skill] <- SkillsMapping.forUser(dbconn, "dustin")  // query can fail
        } yield {

          // simple functional transform to build some arbitrary payload
          // only gets called if both calls succeeded
          allSkills.map{ case (id, skill) =>
            val enabled = userSkills.contains(id)
            UserSkillPicker(id, skill.name, enabled)
          }.toList

        }

      a
    }

    val it = x.run.run // wtf ?
  val logs: List[String] = it._1
    val results: Throwable \/ List[UserSkillPicker] = it._2

    val log: String = logs.mkString("\n")

    results match {
      case -\/(e) => InternalServerError("exception: %s\nlogs: %s".format(e.toString, log))
      case \/-(v) => Ok("success!\nobject: %s\nlogs: %s".format(Json.toJson(v).toString, log))
    }
  }


  object SkillsMapping {

    val mappingWithId =
      get[String]("skills.id") ~
      get[String]("skills.name") ~
      get[String]("skills.description") map {
        case id~name~desc => id -> Skill(name, desc)
      }


    // the return type includes logs, as well as either a throwable or a result
    def all(dbconn: java.sql.Connection): EitherT[MyLogger, Throwable, Map[String, Skill]] = {

      val query = "SELECT skills.id, skills.name FROM skills"

      def results: Throwable \/ Map[String, Skill] = Try(SQL(query).as(mappingWithId *)(dbconn).toMap) match {
        case Failure(e) => \/.left(e)
        case Success(v) => \/.right(v)
      }

      val resultsWithLog: EitherT[MyLogger, Throwable, Map[String, Skill]] =
        for {
          r <- EitherT[MyLogger, Throwable, Map[String, Skill]](Writer(List("running query: %s".format(query)), results))
          _ <- EitherT[MyLogger, Throwable, Int](Writer(List("got results: %s".format(results.toString)), \/.right(0)))
        } yield r

      resultsWithLog
    }



    def forUser(dbconn: java.sql.Connection, userId: String): EitherT[MyLogger, Throwable, Map[String, Skill]] = {

      val query =
        """
          SELECT skills.id, skills.name, skills.description FROM skills
          INNER JOIN skillsets
          ON skills.id = skillsets.skill_id AND skillsets.user_id = {userId}
        """

      def results: Throwable \/ Map[String, Skill] = Try(SQL(query).on('userId -> userId).as(mappingWithId *)(dbconn).toMap) match {
        case Failure(e) => \/.left(e)
        case Success(v) => \/.right(v)
      }

      val resultsWithLog: EitherT[MyLogger, Throwable, Map[String, Skill]] =
        for {
          r <- EitherT[MyLogger, Throwable, Map[String, Skill]](Writer(List("running query: %s".format(query)), results))
          _ <- EitherT[MyLogger, Throwable, Int](Writer(List("got results: %s".format(results.toString)), \/.right(0)))
        } yield r

      resultsWithLog
    }
  }

}