/* eslint-disable */
const path = require("path");
const PACKAGE = require("./package.json");

module.exports = {
    entry: [
        "@babel/polyfill",
        __dirname + "/src/main/webapp/portal/nexus.js",
    ],
    output: {
        filename: "StackV-main.bundle.js",
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
