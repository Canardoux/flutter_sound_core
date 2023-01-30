import Flutter
import AVFAudio
import UIKit
import Foundation

public class SwiftEqualizerPlugin: NSObject, FlutterPlugin {
    let registrar: FlutterPluginRegistrar
    private var audioEngine: AVAudioEngine?
    private var audioUnitEQ: AVAudioUnitEQ?
    private var audioPlayerNode: AVAudioPlayerNode?
    private var audioOutputNode:AVAudioOutputNode?
    private var sessionId:Int?
    private var audioEffects: [EffectData]?
    private var flautoPlayer = FlautoPlayer()
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let instance = SwiftEqualizerPlugin(registrar)
        let channel = FlutterMethodChannel(name: "dev.offcode.equalizer/method", binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    init(_ registrar: FlutterPluginRegistrar) {
        self.registrar = registrar
        super.init()
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "init": initEqualizer(call, result)
        case "audioEffectSetEnabled": enableEffect(call, result)
        case "darwinEqualizerBandSetGain": setEqualizerBandGain(call, result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    func enableEffect(_ call: FlutterMethodCall, _ result: @escaping FlutterResult ) {
        print("Enable Effect!")
        guard let arguments = call.arguments as? Dictionary<String, AnyObject> else {
            result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
            return
        }
        let type = arguments["type"] as! String
        let enabled = arguments["enabled"] as! Bool
        
        print("Set enabled type: \(type) [ \(enabled) ]")
        
        switch type {
        case "DarwinEqualizer":
            audioUnitEQ!.bypass = !enabled
        default:
            result(FlutterError(code: "NOT_INITIALIZED", message: "Not initialized effect \(type)", details: nil))
        }
    }
    
    func setEqualizerBandGain(_ call: FlutterMethodCall, _ result: @escaping FlutterResult ) {
        guard let arguments = call.arguments as? Dictionary<String, AnyObject> else {
            result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
            return
        }
        
        let bandIndex = arguments["bandIndex"] as! Int
        let gain =  Float(arguments["gain"] as! Double)
        print("Set band: \(bandIndex) gain to: \(gain)")
        audioUnitEQ!.bands[bandIndex].gain = gain
    }
    
    public func initEqualizer(_ call: FlutterMethodCall, _ result: @escaping FlutterResult){
        print("Initialize darwin EQ!")
        
        guard let arguments = call.arguments as? Dictionary<String, AnyObject> else {
            result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
            return
        }
        
        print(arguments)
        
        let effectsRaw: [[String: Any?]] =  arguments.keys.contains("DarwinEqualizer") ? (arguments["DarwinEqualizer"] as! [[String: Any?]]) : []
        
        let equalizerRaw = effectsRaw.filter { rawEffect in
            (rawEffect["type"] as! String) == "DarwinEqualizer"
        }.first
        
        if equalizerRaw!["type"] as? String != "DarwinEqualizer" {
            result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no DarwinEqualizer type found", details: nil))
        }
        
        let parameters = equalizerRaw!["parameters"] as! [String: Any]
        let rawBands = parameters["bands"] as! [[String: Any]]
        let enabled = equalizerRaw!["enabled"] as! Bool
        
        let frequenciesAndBands = rawBands.map { map in
            let frequency = map["centerFrequency"] as! Double
            let gain = map["gain"] as! Double
            return (Int(frequency), EqualizerEffectData.gainFrom(Float(gain)))
        }
        
        _ = frequenciesAndBands.map { frequency, _ in
            frequency
        }
        
        let bands = frequenciesAndBands.map { _, band in
            band
        }
        
        if audioEngine == nil {
            print("Setting audio engine!")
            audioEngine = AVAudioEngine()
            
            // add equalizer node
            if audioUnitEQ == nil {
                print("Setting Equalizer!")
                audioUnitEQ = AVAudioUnitEQ(numberOfBands: bands.count) //Set bands count
                
                // Set bands, gains, etc.
                for(i, band) in rawBands.enumerated(){
                    audioUnitEQ!.bands[i].filterType = .parametric
                    audioUnitEQ!.bands[i].frequency = band["centerFrequency"] as! Float
                    audioUnitEQ!.bands[i].bandwidth = 1 // half an octave
                    audioUnitEQ!.bands[i].gain = EqualizerEffectData.gainFrom(band["gain"] as! Float)
                    audioUnitEQ!.bands[i].bypass = false
                }
                audioUnitEQ!.bypass = !enabled //Set enabled
                
                audioEngine!.attach(audioUnitEQ!)
            } else {
                print("EQ already initialized!")
            }
        }else {
            print("Audio engine already running!")
        }
        result("SUCCESS")
    }
}

//    fileprivate func createAudioEffects(_ _audioEffects: [EffectData]) throws {
//
//        for effect in _audioEffects as [any EffectData] {
//          print(effect)
//          if let effect = effect as? EqualizerEffectData {
//              audioUnitEQ = AVAudioUnitEQ(numberOfBands: effect.parameters.bands.count)
//
//              for (i, band) in effect.parameters.bands.enumerated() {
//                  audioUnitEQ!.bands[i].filterType = .parametric
//                  audioUnitEQ!.bands[i].frequency = band.centerFrequency
//                  audioUnitEQ!.bands[i].bandwidth = 1 // half an octave
//                  audioUnitEQ!.bands[i].gain = EqualizerEffectData.gainFrom(band.gain)
//                  audioUnitEQ!.bands[i].bypass = false
//              }
//
//              audioUnitEQ!.bypass = !effect.enabled
//          } else {
//              throw NotSupportedError(value: effect.type, "When initialize effect")
//          }
//      }
//
//     func parse(from map: [String: Any?]) throws {
//        if map["type"] as? String != "DarwinEqualizer" {
//            return nil
//        }
//
//        let parameters = map["parameters"] as! [String: Any]
//
//        let rawBands = parameters["bands"] as! [[String: Any]]
//        let frequenciesAndBands = rawBands.map { map in
//            let frequency = map["centerFrequency"] as! Double
//            let gain = map["gain"] as! Double
//            return (Int(frequency), EqualizerEffectData.gainFrom(Float(gain)))
//        }
//
//        let frequencies = frequenciesAndBands.map { frequency, _ in
//            frequency
//        }
//
//        let bands = frequenciesAndBands.map { _, band in
//            band
//        }

//        let equalizer = try Equalizer(frequencies: frequencies, preSets: [bands])

//        try equalizer.activate(preset: 0)

//        return equalizer
//    }
//  }

    
    
    //===================================================================================================================//
    
    // public func setAudiosessionId(_ call:FlutterMethodCall, _ result: @escaping FlutterResult){
    //     guard let arguments = call.arguments as? Dictionary<String, AnyObject> else {
    //          result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
    //          return
    //     }
    //     audioEngine = AVAudioEngine()
    //     audioUnitEQ = AVAudioUnitEQ()
    //     audioPlayerNode = AVAudioPlayerNode()

    //     let audioSession = AVAudioSession.sharedInstance()
    //     do {
    //         try audioSession.setCategory(.playAndRecord, options: .defaultToSpeaker)
    //     }catch let error as NSError{
    //         print("ERROR:", error)
    //     }
    //     do{
    //         try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
    //     }catch let error as NSError {
    //        print("ERROR:", error)
    //     }

    //     print("audioSession: \(audioSession)")

    //     sessionId = arguments["sessionId"] as? Int

    //     print("== Set audio session id to: \(String(describing: sessionId))")
    // }
    








//        let numberOfBands = arguments["numberOfBands"] as? Int
//        let freqs = arguments["freqs"] as? Array<Int>
//        // in viewDidLoad():
//        audioUnitEQ = AVAudioUnitEQ(numberOfBands: numberOfBands!)
//        audioEngine?.attach(audioPlayerNode!)
//        audioEngine?.attach(audioUnitEQ!)
//        let bands = audioUnitEQ!.bands
//        audioEngine!.connect(audioPlayerNode!, to: audioUnitEQ!, format: nil)
//        audioEngine!.connect(audioUnitEQ!, to: audioEngine!.outputNode, format: nil)
//        for i in 0...(bands.count - 1) {
//            bands[i].frequency  = Float(freqs![i])
//            bands[i].bypass     = false
//            //            bands[i].filtertype = .parametric
//        }
//
//        bands[0].gain = -10.0
//        bands[0].filterType = .lowShelf
//        bands[1].gain = -10.0
//        bands[1].filterType = .lowShelf
//        bands[2].gain = -10.0
//        bands[2].filterType = .lowShelf
//        bands[3].gain = 10.0
//        bands[3].filterType = .highShelf
//        bands[4].gain = 10.0
//        bands[4].filterType = .highShelf
//
//        do {
//            if let filepath = Bundle.main.path(forResource: "song", ofType: "mp3") {
//                let filepathURL = NSURL.fileURL(withPath: filepath)
//                do {
//                    audioFile = try AVAudioFile(forReading: filepathURL)
//                } catch {
//                    print(error)
//                }
//
//                audioEngine.prepare()
//                do{
//                    try audioEngine.start()
//                }catch{
//                    print(error)
//                }
//                audioPlayerNode.scheduleFile(audioFile, at: nil, completionHandler: nil)
//                audioPlayerNode.play()
//            }
//        }
    
//    fileprivate func createAudioEffects(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) throws {
//        guard let arguments = call.arguments as? Dictionary<String, AnyObject> else {
//             result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
//             return
//        }
//
//
//        for effect in audioEffects {
//            if let effect = effect as? EqualizerEffectData {
//                audioUnitEQ = AVAudioUnitEQ(numberOfBands: effect.parameters.bands.count)
//
//                for (i, band) in effect.parameters.bands.enumerated() {
//                    audioUnitEQ!.bands[i].filterType = .parametric
//                    audioUnitEQ!.bands[i].frequency = band.centerFrequency
//                    audioUnitEQ!.bands[i].bandwidth = 1 // half an octave
//                    audioUnitEQ!.bands[i].gain = Util.gainFrom(band.gain)
//                    audioUnitEQ!.bands[i].bypass = false
//                }
//
//                audioUnitEQ!.bypass = !effect.enabled
//            } else {
//                throw NotSupportedError(value: effect.type, "When initialize effect")
//            }
//        }
//    }
    

