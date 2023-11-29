
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNEsptouchSpec.h"

@interface Esptouch : NSObject <NativeEsptouchSpec>
#else
#import <React/RCTBridgeModule.h>

@interface Esptouch : NSObject <RCTBridgeModule>
#endif

@end
