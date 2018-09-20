const path = require("path");
const CleanWebpackPlugin = require("clean-webpack-plugin");

module.exports = {
    mode: "production",
    entry: {
        babel: "@babel/polyfill",
        "StackV-main": __dirname + "/src/main/webapp/portal/nexus.js",
    },
    output: {
        filename: "[name].bundle.js",
        //chunkFilename: "[name].bundle.js",
        publicPath: "/StackV-web/js/bundles/",
        path: __dirname + "/src/main/webapp/js/bundles/"
    },
    resolve: {
        extensions: [".js", ".jsx"]
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: {
                    test: path.resolve(__dirname, "node_modules"),
                    exclude: path.resolve(__dirname, "node_modules/stackv-visualization")
                },
                use: [
                    {
                        loader: "babel-loader",
                        options: {
                            babelrc: false,
                            presets: ["@babel/preset-env", ["@babel/preset-stage-0", { "decoratorsLegacy": true }], "@babel/preset-react"],
                            plugins: [
                                ["@babel/plugin-proposal-object-rest-spread", { "useBuiltIns": true }],
                                ["@babel/transform-runtime"],
                                ["transform-class-properties"],
                                ["@babel/plugin-proposal-class-properties", { "loose": false }],
                                ["emotion"]
                            ]
                        }
                    }
                ]
            }
        ]
    },
    plugins: [
        new CleanWebpackPlugin("src/main/webapp/js/bundles/")
    ]
};
