/*global process, require */

(function () {

    "use strict";

    var args = process.argv,
        fs = require("fs"),
        less = require("less"),
        mkdirp = require("mkdirp"),
        path = require("path");

    var SOURCE_FILE_MAPPINGS_ARG = 2;
    var TARGET_ARG = 3;
    var OPTIONS_ARG = 4;

    var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
    var target = args[TARGET_ARG];
    var options = JSON.parse(args[OPTIONS_ARG]);

    var sourcesToProcess = sourceFileMappings.length;
    var results = [];
    var problems = [];

    function parseDone() {
        if (--sourcesToProcess === 0) {
            console.log("\u0010" + JSON.stringify({results: results, problems: problems}));
        }
    }

    function throwIfErr(e) {
        if (e) throw e;
    }

    sourceFileMappings.forEach(function (sourceFileMapping) {

        var input = sourceFileMapping[0];
        var outputFile = sourceFileMapping[1].replace(".less", options.compress ? ".min.css" : ".css");
        var output = path.join(target, outputFile);
        var sourceMapOutput = output + ".map";

        fs.readFile(input, "utf8", function (e, contents) {
            throwIfErr(e);

            var writeSourceMap = function (content) {
              
                if (options.relativeImports) {
                    // replace leading part in included assets with "../"
                    content = JSON.parse(content);
                    for (var i = 0, s = content.sources, l = s.length; i < l; i++) {
                        options.paths.forEach(function (path) {
                            // for windows replace \ with /
                            path = path.replace(/\\/g, "/");
                            if (path[path.length - 1] !== "/")
                                path += "/";
                            if (s[i].substr(0, path.length) === path)
                                s[i] = "../" + s[i].substr(path.length);
                        });
                    }
                    content = JSON.stringify(content);
                }
              
                mkdirp(path.dirname(sourceMapOutput), function (e) {
                    throwIfErr(e);
                    fs.writeFile(sourceMapOutput, content, "utf8", throwIfErr);
                });
            };

            var contentWithVars = (options.globalVariables ? options.globalVariables + "\n" : "") +
                contents +
                (options.modifyVariables ? "\n" + options.modifyVariables : "");

            options.filename = input; // Yuk, but I can't be bothered copying as there is no easy way in JS.
            var parser = new (less.Parser)(options);

            function handleLessError(e) {
                if (e.line != undefined && e.column != undefined) {
                    problems.push({
                        message: e.message,
                        severity: "error",
                        lineNumber: e.line,
                        characterOffset: e.column,
                        lineContent: contentWithVars.split("\n")[e.line - 1],
                        source: input
                    });
                } else {
                    throw e;
                }
                results.push({
                    source: input,
                    result: null
                });

                parseDone();
            }

            parser.parse(contentWithVars, function (e, tree) {
                if (e) {
                    handleLessError(e);
                } else {

                    try {
                        var css = tree.toCSS({
                            cleancss: options.cleancss,
                            cleancssOptions: options.cleancssOptions || {},
                            compress: options.compress,
                            ieCompat: options.ieCompat || true,
                            maxLineLen: options.maxLineLen,
                            outputSourceFiles: options.outputSourceFiles,
                            relativeUrls: options.relativeUrls,
                            rootpath: options.rootPath || "",
                            silent: options.silent,
                            sourceMap: options.sourceMap,
                            sourceMapBasepath: path.dirname(input),
                            sourceMapFilename: path.basename(sourceMapOutput),
                            sourceMapOutputFilename: path.basename(outputFile),
                            strictMath: options.strictMath,
                            strictUnits: options.strictUnits,
                            urlArgs: options.urlArgs || "",
                            verbose: options.verbose,
                            writeSourceMap: writeSourceMap
                        });

                        mkdirp(path.dirname(output), function (e) {
                            throwIfErr(e);

                            fs.writeFile(output, css, "utf8", function (e) {
                                throwIfErr(e);

                                var imports = [];
                                var files = parser.imports.files;
                                for (var file in files) {
                                    if (files.hasOwnProperty(file)) {
                                        imports.push(file);
                                    }
                                }

                                results.push({
                                    source: input,
                                    result: {
                                        filesRead: [input].concat(imports),
                                        filesWritten: options.sourceMap ? [output, sourceMapOutput] : [output]
                                    }
                                });

                                parseDone();
                            });
                        });
                    } catch (e) {
                        handleLessError(e);
                    }
                }
            });
        });
    });
})();