const path = require("path");
const PACKAGE = require("./package.json");

module.exports = {
    mode: "production",
    entry: {
        babel: "@babel/polyfill",
        "StackV-main": __dirname + "/src/main/webapp/portal/nexus.js",
    },
    output: {
        filename: "[name].bundle.js",
        chunkFilename: "[name].bundle.js",
        publicPath: "/StackV-web/js/",
        path: __dirname + "/src/main/webapp/js/"
    },
    resolve: {
        extensions: [".js"]
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components)/,
                use: [
                    {
                        loader: "babel-loader",
                        options: {
                            babelrc: false,
                            presets: [
                                "@babel/preset-env",
                                [ "@babel/preset-stage-0", {"decoratorsLegacy": true} ]
                            ],
                            plugins: [
                                ["@babel/syntax-dynamic-import"],
                                ["@babel/plugin-proposal-object-rest-spread", { "useBuiltIns": true }],
                                ["@babel/transform-runtime"],
                                ["@babel/plugin-proposal-class-properties", { "loose": false }],
                            ]
                        }
                    }
                ]
            }
        ]
    }
};
