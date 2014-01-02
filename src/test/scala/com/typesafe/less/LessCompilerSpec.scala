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
import _root_.sbt._
import scala.collection.immutable
import akka.actor.ActorSystem
import spray.json._
import com.typesafe.jse.Trireme

@RunWith(classOf[JUnitRunner])
class LessCompilerSpec extends Specification with NoTimeConversions {

  implicit val duration = 15.seconds
  implicit val timeout = Timeout(duration)

  sequential

  "the less compiler" should {
    "compile a trivial file" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less", dir / "a.less")
        val acss = dir / "a.css"
        val result = compile(dir, LessOptions(), aless -> acss)
        result.headOption must beSome.like {
          case LessSuccess(file, depends) =>
            file must_== aless.getAbsolutePath
            depends must beEmpty
        }
        acss.exists() must beTrue
      }
    }

    "report errors correctly for a file with errors in it" in new TestActorSystem {
      withTmpDir { dir =>
        val badless = resourceToFile("bad.less", dir / "bad.less")
        val result = compile(dir, LessOptions(), badless -> dir / "bad.css")
        result.headOption must beSome.like {
          case LessError(file, errors) =>
            file must_== badless.getAbsolutePath
            val err = errors.head
            err.filename must beSome(badless.getAbsolutePath)
            err.line must beSome(4)
            err.column must beSome(2)
        }
      }
    }

    "report errors correctly for a missing file" in new TestActorSystem {
      withTmpDir { dir =>
        val missingless = dir / "missing.less"
        val result = compile(dir, LessOptions(), missingless -> dir / "missing.css")
        result.headOption must beSome.like {
          case LessError(file, errors) =>
            file must_== missingless.getAbsolutePath
            val err = errors.head
            err.message must_== "File not found"
        }
      }
    }

    "report errors correctly for bad rendering" in new TestActorSystem {
      withTmpDir { dir =>
        val badrender = resourceToFile("badrender.less", dir / "badrender.less")
        val result = compile(dir, LessOptions(), badrender -> dir / "badrender.css")
        result.headOption must beSome.like {
          case LessError(file, errors) =>
            file must_== badrender.getAbsolutePath
            val err = errors.head
            err.filename must beSome(badrender.getAbsolutePath)
            err.line must beSome(2)
            err.column must beSome(9)
        }
      }
    }

    "support files with imports" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less", dir / "a.less")
        val importless = resourceToFile("import.less", dir / "import.less")
        val css = dir / "import.css"
        val result = compile(dir, LessOptions(), importless -> css)
        result.headOption must beSome.like {
          case LessSuccess(file, depends) =>
            file must_== importless.getAbsolutePath
            depends must containTheSameElementsAs(Seq(aless.getAbsolutePath))
        }
        css.exists() must beTrue
      }      
    }

    "support source maps" in new TestActorSystem {
      withTmpDir { dir =>
        // Use different input/output dir
        val aless = resourceToFile("a.less", dir / "in" / "a.less")
        val sourceMap = dir / "out" / "a.css.map"
        val css = dir / "out" / "a.css"
        val result = compile(dir, LessOptions(sourceMap = true), aless -> css)
        result.head must beAnInstanceOf[LessSuccess]
        sourceMap.exists() must beTrue

        // Check that the correct base path was used
        import DefaultJsonProtocol._
        val map = JsonParser(IO.read(sourceMap)).asJsObject
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
        val aless = resourceToFile("a.less", dir / "a.less")
        val out = dir / "a.min.css"
        val result = compile(dir, LessOptions(compress = true, sourceMap = false), aless -> out)
        result.head must beAnInstanceOf[LessSuccess]

        out.length must beLessThan(aless.length)
      }
    }

    "support compiling multiple files" in new TestActorSystem {
      withTmpDir { dir =>
        val aless = resourceToFile("a.less", dir / "a.less")
        val acss = dir / "a.css"
        val bless = resourceToFile("b.less", dir / "b.less")
        val bcss = dir / "b.css"
        val result = compile(dir, LessOptions(compress = true), aless -> acss, bless -> bcss)

        result must haveSize(2)
        result.find(_.inputFile == aless.getAbsolutePath) must beSome.like {
          case s: LessSuccess =>
            acss.exists() must beTrue
            IO.read(acss) must contain("h1")
        }
        result.find(_.inputFile == bless.getAbsolutePath) must beSome.like {
          case s: LessSuccess =>
            bcss.exists() must beTrue
            IO.read(bcss) must contain("h2")
        }

      }
    }

    "support importing files from other paths" in new TestActorSystem {
      withTmpDir { dir =>
        val includePath = dir / "includes"
        val aless = resourceToFile("a.less", includePath / "a.less")
        val mainPath = dir / "main"
        val importless = resourceToFile("import.less", mainPath / "import.less")
        val css = dir / "out" / "import.css"

        val result = compile(dir, LessOptions(includePaths = Seq(includePath)), importless -> css)

        result.headOption must beSome.like {
          case LessSuccess(file, depends) =>
            file must_== importless.getAbsolutePath
            depends must containTheSameElementsAs(Seq(aless.getAbsolutePath))
        }
        css.exists() must beTrue
      }
    }

  }

  def withTmpDir[T](block: File => T) = {
    val dir = createTmpDir()
    try {
      block(dir)
    } finally {
      def delete(file: File): Unit = file match {
        case dir if dir.isDirectory =>
          dir.listFiles().foreach(delete)
          dir.delete()
        case file => file.delete()
      }
      delete(dir)
    }
  }

  def createTmpDir() = {
    val dir = File.createTempFile("less-compiler-spec", "")
    dir.delete()
    dir.mkdir()
    dir
  }

  def createCompiler(dir: File)(implicit system: ActorSystem) = {
    val extractor = new WebJarExtractor(this.getClass.getClassLoader)
    extractor.extractWebJarTo("less", dir)
    extractor.extractWebJarTo("source-map", dir)
    extractor.extractWebJarTo("amdefine", dir)
    val lessc = resourceToFile("lessc.js", dir / "lessc.js")
    val engine = system.actorOf(Trireme.props(), "engine")
    new LessCompiler(engine, lessc.getAbsoluteFile, List((dir / "lib").getAbsolutePath))
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

  def resourceToFile(resource: String, to: File): File = {
    val is = this.getClass.getClassLoader.getResourceAsStream(resource)
    try {
      IO.transfer(is, to)
      to
    } finally {
      is.close()
    }
  }

}

