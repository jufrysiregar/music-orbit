import SwiftUI
import shared

/**
 * ContentView mounts the Kotlin Multiplatform shared module as a Compose UI view controller.
 * The shared framework is built via the KMP Gradle plugin (iosArm64 / iosSimulatorArm64).
 * Full iOS implementation will be added in a future sprint.
 */
struct ContentView: View {
    var body: some View {
        // Placeholder until Compose Multiplatform iOS entry point is wired
        VStack {
            Image(systemName: "music.note")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Music Orbit")
                .font(.headline)
            Text("iOS implementation coming soon")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
