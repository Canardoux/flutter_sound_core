import Foundation

enum EffectType: String, Codable {
    case darwinEqualizer = "DarwinEqualizer"
}

protocol EffectData {
    var type: EffectType { get }
}

struct BandEqualizerData: Codable {
    let index: Int
    let centerFrequency: Float
    let gain: Float
}

struct ParamsEqualizerData: Codable {
    let bands: [BandEqualizerData]
}

struct EqualizerEffectData: EffectData, Codable {
    let type: EffectType
    let enabled: Bool
    let parameters: ParamsEqualizerData

    static func fromJson(_ map: [String: Any]) -> EqualizerEffectData {
        return try! JSONDecoder().decode(EqualizerEffectData.self, from: JSONSerialization.data(withJSONObject: map))
    }

    static func effectFrom(_ map: [String: Any]) throws -> EffectData {
        let type = map["type"] as! String
        switch type {
        case EffectType.darwinEqualizer.rawValue:
            return EqualizerEffectData.fromJson(map)
        default:
            throw NotSupportedError(value: type, "When decoding effect")
        }
    }
    
    static func gainFrom(_ value: Float) -> Float {
        // Equalize the level between iOS and android
        return value * 2.8
    }
}


class NotSupportedError: PluginError {
    var value: Any

    init(value: Any, _ message: String) {
        self.value = value
        super.init(400, "Not support \(value)\n\(message)")
    }
}

class PluginError: Error {
    var code: Int
    var message: String

    init(_ code: Int, _ message: String) {
        self.code = code
        self.message = message
    }
}

