package com.typesafe.less

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.webjars.WebJarExtractor
import akka.util.Timeout
import scala.concurrent.duration._
import org.specs2.time.NoTimeConversions
import java.io.File
import scala.concurrent.Await
import scala.collection.immutable
import akka.actor.ActorSystem
import spray.json._
import com.typesafe.jse.Trireme
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class LessCompilerSpec extends Specification with NoTimeConversions {

  implicit val duration = 15.seconds
  implicit val timeout = Timeout(duration)

  sequential

  "the less compiler" should {
    "compile a trivial file" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less")
        val acss = new File(dir, "a.css")
        val result = compile(dir, LessOptions(), aless -> acss)
        result.headOption must beSome.like {
          case LessSuccess(inputFile, outputFile, depends) =>
            inputFile must_== aless
            outputFile must_== acss
            depends must beEmpty
        }
        acss.exists() must beTrue
      }
    }

    "report errors correctly for a file with errors in it" in new TestActorSystem {
      withTmpDir { dir =>
        val badless = resourceToFile("bad.less")
        val result = compile(dir, LessOptions(), badless -> new File(dir, "bad.css"))
        result.headOption must beSome.like {
          case LessError(inputFile, outputFile, errors) =>
            inputFile must_== badless
            val err = errors.head
            err.filename must beSome(badless)
            err.line must beSome(4)
            err.column must beSome(2)
        }
      }
    }

    "report errors correctly for a missing file" in new TestActorSystem {
      withTmpDir { dir =>
        val missingless = new File(dir, "missing.less")
        val result = compile(dir, LessOptions(), missingless -> new File(dir, "missing.css"))
        result.headOption must beSome.like {
          case LessError(inputFile, outputFile, errors) =>
            inputFile must_== missingless
            val err = errors.head
            err.message must_== "File not found"
        }
      }
    }

    "report errors correctly for bad rendering" in new TestActorSystem {
      withTmpDir { dir =>
        val badrender = resourceToFile("badrender.less")
        val result = compile(dir, LessOptions(), badrender -> new File(dir, "badrender.css"))
        result.headOption must beSome.like {
          case LessError(inputFile, outputFile, errors) =>
            inputFile must_== badrender
            val err = errors.head
            err.filename must beSome(badrender)
            err.line must beSome(2)
            err.column must beSome(9)
        }
      }
    }

    "support files with imports" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less")
        val importless = resourceToFile("import.less")
        val css = new File(dir, "import.css")
        val result = compile(dir, LessOptions(), importless -> css)
        result.headOption must beSome.like {
          case LessSuccess(inputFile, outputFile, depends) =>
            inputFile must_== importless
            depends must containTheSameElementsAs(Seq(aless))
        }
        css.exists() must beTrue
      }      
    }

    "support source maps" in new TestActorSystem {
      withTmpDir { dir =>
        // Use different input/output dir
        val aless = resourceToFile("a.less")
        val sourceMap = new File(dir, "out" + File.separator + "a.css.map")
        val css = new File(dir, "out" + File.separator + "a.css")
        val result = compile(dir, LessOptions(sourceMap = true), aless -> css)
        result.head must beAnInstanceOf[LessSuccess]
        sourceMap.exists() must beTrue

        // Check that the correct base path was used
        import DefaultJsonProtocol._
        val map = JsonParser(Source.fromFile(sourceMap).mkString).asJsObject
        map.fields("sources").convertTo[JsArray].elements.head must_== JsString("a.less")

        // Check that the correct base path was used in the file
        // Disabled until https://github.com/less/less.js/issues/1644 is fixed.

        //println(IO.read(css))
        //"""/\*# sourceMappingURL=(.*) \*/""".r.findFirstMatchIn(IO.read(css)) must beSome.like {
        //  case groups => groups.group(1) must_== "a.css.map"
        //}

      }
    }

    "support compression" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less")
        val out = new File(dir, "a.min.css")
        val result = compile(dir, LessOptions(compress = true, sourceMap = false), aless -> out)
        result.head must beAnInstanceOf[LessSuccess]

        out.length must beLessThan(aless.length)
      }
    }

    "support compiling multiple files" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less")
        val acss = new File(dir, "a.css")
        val bless = resourceToFile("b.less")
        val bcss = new File(dir, "b.css")
        val result = compile(dir, LessOptions(compress = true), aless -> acss, bless -> bcss)

        result must haveSize(2)
        result.find(_.input == aless) must beSome.like {
          case s: LessSuccess =>
            acss.exists() must beTrue
            Source.fromFile(acss).mkString must contain("h1")
        }
        result.find(_.input == bless) must beSome.like {
          case s: LessSuccess =>
            bcss.exists() must beTrue
            Source.fromFile(bcss).mkString must contain("h2")
        }

      }
    }

    "support importing files from other paths" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less")
        val importless = resourceToFile("import.less")
        val css = new File(dir, "out" + File.separator + "import.css")

        val result = compile(dir, LessOptions(includePaths = Seq(dir)), importless -> css)

        result.headOption must beSome.like {
          case LessSuccess(inputFile, outputFile, depends) =>
            inputFile must_== importless
            depends must containTheSameElementsAs(Seq(aless))
        }
        css.exists() must beTrue
      }
    }

  }

  def createTmpDir() = {
    val dir = File.createTempFile("less-compiler-spec", "")
    dir.delete()
    dir.mkdir()
    dir
  }

  def withTmpDir[T](block: File => T) = {
    val dir = createTmpDir()
    try {
      block(dir)
    } finally {
      def delete(file: File): Unit = file match {
        case d if d.isDirectory =>
          d.listFiles().foreach(delete)
          d.delete()
        case f => f.delete()
      }
      delete(dir)
    }
  }

  def createCompiler(dir: File)(implicit system: ActorSystem): LessCompiler = {
    val extractor = new WebJarExtractor(this.getClass.getClassLoader)
    extractor.extractAllNodeModulesTo(dir)
    val engine = system.actorOf(Trireme.props(stdModulePaths = immutable.Seq(dir.getCanonicalPath)), "engine")
    new LessCompiler(engine, resourceToFile("lessc.js"))
  }

  def compile(dir: File, options: LessOptions, files: (File, File)*)(implicit system: ActorSystem): Seq[LessResult] = {
    val result = Await.result(createCompiler(dir).compile(immutable.Seq(files:_*), options), duration)
    if (!result.stdout.isEmpty) {
      println(result.stdout)
    }
    if (!result.stderr.isEmpty) {
      System.err.print(result.stderr)
    }
    result.results
  }

  def resourceToFile(name: String): File = {
    new File(this.getClass.getClassLoader.getResource(name).toURI)
  }

}

