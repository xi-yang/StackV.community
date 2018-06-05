module.exports = function(grunt)
{
    require("load-grunt-tasks")(grunt);

    grunt.initConfig({
        babel: {
            options: {
                sourceMap: true,
                presets: ["babel-preset-es2015"]
            },
            dist: {
                files: [
                    {
                        expand: true,
                        cwd: "src/main/webapp/js",
                        src: ["*.js", "**/*.js", "!**/libs/**"],
                        dest: "target/dist/js"
                    }
                ]
            }
        },

        uglify: {
            build: {
                files: [{
                    expand: true,
                    cwd: "target/StackV-web-1.0-SNAPSHOT/js",
                    src: ["*.js", "**/*.js", "!**/libs/**"],
                    dest: "target/dist/js"
                }]
            }
        }
    });

    grunt.registerTask("default", ["babel", "uglify"]);
};
