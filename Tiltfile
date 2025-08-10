# Build
custom_build(
  ref = 'order-service',
  command = './gradlew bootBuildImage --imageName $EXPECTED_REF',
  deps = ['build.gradle.kts', 'src']
)

# Deploy
k8s_yaml(['k8s/deployment.yaml', 'k8s/service.yaml'])

# Manage
k8s_resource('order-service', port_forwards=['9002']
