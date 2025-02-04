// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginRfid",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorPluginRfid",
            targets: ["RFIDPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "RFIDPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/RFIDPlugin"),
        .testTarget(
            name: "RFIDPluginTests",
            dependencies: ["RFIDPlugin"],
            path: "ios/Tests/RFIDPluginTests")
    ]
)