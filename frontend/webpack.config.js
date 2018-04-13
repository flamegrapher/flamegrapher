var path = require("path");
var webpack = require("webpack");
const ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {
  entry: "./src/main.js",
  output: {
    path: path.resolve(__dirname, "./dist"),
    publicPath: "/dist/",
    filename: "build.js"
  },
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: "vue-loader",
        options: {
          loaders: {
          }
          // other vue-loader options go here
        }
      },
      {
        test: /\.js$/,
        loader: "babel-loader",
        exclude: /node_modules/
      },
      {
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          fallback: "style-loader",
          use: "css-loader"
        })
      },
      {
        test: /\.(png|jpg|gif|svg)$/,
        loader: "file-loader",
        options: {
          name: "[name].[ext]?[hash]"
        }
      }
    ]
  },
  resolve: {
    alias: {
      "vue$": "vue/dist/vue.esm.js"
    }
  },
  devServer: {
    historyApiFallback: true,
    noInfo: true,
    port: 3000,
    proxy: {
      "/api/list/": {
        target: "http://localhost:8080/api/list",
        secure: false,
        ws: false
      },
      "/api/start/": {
        target: "http://localhost:8080/api/start/",
        secure: false,
        ws: true
      },
      "/api/stop/": {
        target: "http://localhost:8080/api/stop/",
        secure: false,
        ws: true
      },
      "/api/dump/": {
        target: "http://localhost:8080/api/dump/",
        secure: false,
        ws: true
      },
      "/api/flames/": {
        target: "http://localhost:8080/api/flames/",
        secure: false,
        ws: true
      }
    }
  },
  performance: {
    hints: false
  },
  plugins: [new ExtractTextPlugin("main.css")],
  devtool: "#eval-source-map"
};

if (process.env.NODE_ENV === "production") {
  module.exports.devtool = "#source-map";
  // http://vue-loader.vuejs.org/en/workflow/production.html
  module.exports.plugins = (module.exports.plugins || []).concat([
    new webpack.DefinePlugin({
      "process.env": {
        NODE_ENV: '"production"'
      }
    }),
    new webpack.optimize.UglifyJsPlugin({
      sourceMap: true,
      compress: {
        warnings: false
      }
    }),
    new webpack.LoaderOptionsPlugin({
      minimize: true
    })
  ]);
}
