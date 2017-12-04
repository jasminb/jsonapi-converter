@Library('SQE-CI') _


import com.nbcuni.concerto.ConcertoDCJsonapiConverter

def context = getBuildContext{}
def concerto = new ConcertoDCJsonapiConverter(this, context)

timestamps {
    concerto.prepareEnv()
    switch (context.BUILD_PIPELINE) {
        case "release":
            concerto.runSonar()
            concerto.artifactBuild()
            concerto.releaseBuild()
            break
        default:
            throw new Exception("No pipeline defined.")
            break
    }
    concerto.sendFinalNotification()
}