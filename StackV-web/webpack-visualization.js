const path = require("path");
const PACKAGE = require("./package.json");

module.exports = {
    entry: [
        "@babel/polyfill",
        __dirname + "/src/main/webapp/visual/rework.js",
    ],
    output: {
        filename: "StackV-visualization.bundle.js",
        path: __dirname + "/src/main/webapp/js/",
        library: "SVGraphic",
        libraryTarget: "umd",
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
    },
    externals: {
        "d3": "d3",
        "lodash": "_",
        "lz-string": "LZString",
    },
};
