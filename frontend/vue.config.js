module.exports = {
  devServer: {
    port: 8081,
    client: {
      overlay: {
        errors: true,
        warnings: false,
        runtimeErrors: false
      }
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
