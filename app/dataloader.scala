import controllers.Root._
import play.api.Play
import play.api.db.DB
import java.util.UUID.randomUUID
import scala.util.Try


object dataloader {

  val skills = List(
    "Web Designer",
    "Objective C Developer",
    "Android Developer",
    "Classical Guitar",
    "Jazz Guitar",
    "Classical Piano",
    "Jazz Piano",
    "Composer",
    "Conductor",
    "Photographer",
    "Graphic Designer",
    "Oboe",
    "Clarinet",
    "Trumpet",
    "Horn",
    "English Horn",
    "Viola",
    "Violin",
    "Cello",
    "Bass",
    "Video Editor",
    "Painter",
    "Sculptor",
    "Dancer",
    "Choreographer",
    "Producer",
    "Lighting Designer",
    "Director"
  )

  def main(args: Array[String]) {
    import play.api.Play.current
    //implicit val app = FakeApplication()
    //Play.start(app)

    Try(DB.withTransaction { dbconn =>
      skills.foreach { skillName =>
        val o = Skill(skillName, "dummy description for %s".format(skillName))
        SkillsMapping.create(dbconn, randomUUID().toString, o)
      }
    })

    //Play.stop()
  }
}
