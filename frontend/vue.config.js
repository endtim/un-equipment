module.exports = {
  devServer: {
    port: 8081,
    client: {
      overlay: false
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
