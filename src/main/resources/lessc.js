(function (externalArgs) {

    var path = require('path'),
        fs = require('fs'),
        os = require('os'),
        mkdirp;


    var args;
    try {
        args = require("system").args;
    } catch (e) {
        args = Array.prototype.slice.call(externalArgs);
        args.unshift("", "");
    }

    // Import less, expects it to be in the module path somewhere
    var less = require("less/index");

    var ensureDirectory = function (filepath) {
        var dir = path.dirname(filepath),
            cmd,
            existsSync = fs.existsSync || path.existsSync;
        if (!existsSync(dir)) {
            if (mkdirp === undefined) {
                try {mkdirp = require('mkdirp');}
                catch(e) { mkdirp = null; }
            }
            cmd = mkdirp && mkdirp.sync || fs.mkdirSync;
            cmd(dir);
        }
    };

    var jobs = JSON.parse(args[2]);
    var results = [];

    // Called when less has finished parsing a file
    var finishParsing = function(result) {
        results.push(result);
        if (jobs.length == results.length) {
            // If all files are passed, write the results to standard out
            console.log(JSON.stringify(results));
        }
    };

    // Called when an error is encountered
    var reportError = function(input, err) {
        finishParsing({status: "failure", inputFile: input, compileErrors: [err]})
    };

    var doJob = function(options) {
        var input = options.input;
        if (options.verbose) {
            console.log("Compiling " + input);
        }

        var output = options.output;

        if (!options.sourceMapFileInline) {
            var writeSourceMap = function(output) {
                var filename = options.sourceMapFilename;
                ensureDirectory(filename);
                fs.writeFileSync(filename, output, 'utf8');
            };
        }

        // Most of this is adapted from the less bin/less script
        var parseLessFile = function (e, data) {

            if (e) {
                reportError(input, {message: "File not found"});
                return;
            }

            data = options.globalVariables + data + options.modifyVariables;

            options.paths = [path.dirname(input)].concat(options.paths);
            options.filename = input;

            var parser = new(less.Parser)(options);
            parser.parse(data, function (err, tree) {
                if (err) {
                    reportError(input, err);
                } else {
                    try {
                        var css = tree.toCSS({
                            silent: options.silent,
                            verbose: options.verbose,
                            ieCompat: options.ieCompat,
                            compress: options.compress,
                            cleancss: options.cleancss,
                            sourceMap: options.sourceMap,
                            sourceMapFilename: options.sourceMapFilename,
                            sourceMapURL: options.sourceMapURL,
                            sourceMapOutputFilename: options.sourceMapOutputFilename,
                            sourceMapBasepath: options.sourceMapBasepath,
                            sourceMapRootpath: options.sourceMapRootpath || "",
                            outputSourceFiles: options.outputSourceFiles,
                            writeSourceMap: writeSourceMap,
                            maxLineLen: options.maxLineLen,
                            strictMath: options.strictMath,
                            strictUnits: options.strictUnits
                        });
                        ensureDirectory(output);
                        fs.writeFileSync(output, css, 'utf8');
                        if (options.verbose) {
                            console.log("Wrote " + output);
                        }

                        var imports = [];
                        for (filename in parser.imports.files) {
                            imports.push(filename);
                        }

                        finishParsing({status: "success", inputFile: input, dependsOn: imports})
                    } catch (e) {
                        reportError(input, e);
                    }
                }
            });
        };

        fs.readFile(input, 'utf8', parseLessFile);

    };

    for (var i = 0; i < jobs.length; i++) {
        doJob(jobs[i]);
    }
}(arguments));
