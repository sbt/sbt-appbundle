resolvers += Resolver.url( "scalasbt", url( "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases" ))( Resolver.ivyStylePatterns )

addSbtPlugin( "de.sciss" % "sbt-appbundle" % "0.13" )
