## WKWebView Component for React Native

Thanks Ruoyu Sun for his great job. We fork `react-native-wkwebview-reborn` project for adding `initialJavaScript` prop to android WebView. The `initialJavaScirpt` prop receives a string, and will run the script at dom starting. For more info about the feature of WKWebView, Pls check the [react-native-wkwebview-reborn](https://github.com/CRAlpha/react-native-wkwebview). To implement injecting JavaScript at document starting, we 'steal' a lot things from TrustWallet Web3View. Thanks TrustWallet

### Install

1. Install from npm (note the postfix in the package name): `npm install https://www.github.com/consenlabs/react-native-wkwebview`
2. run `react-native link react-native-wkwebview`

**Manual alternative**

Install from npm (note the postfix in the package name): `npm install https://www.github.com/consenlabs/react-native-wkwebview`

iOS:
1. In the XCode's "Project navigator", right click on your project's Libraries folder ➜ Add Files to <...>
2. Go to node_modules ➜ react-native-wkwebview-reborn ➜ ios ➜ select `RCTWKWebView.xcodeproj`
3. Go your build target ➜ Build Phases ➜ Link Binary With Libraries, click "+" and select `libRCTWkWebView.a` (see the following screenshot for reference)
![Linking](https://user-images.githubusercontent.com/608221/28060167-0650e3f4-6659-11e7-8085-7a8c2615f90f.png)
4. Compile and profit (Remember to set Minimum Deployment Target = 8.0)

Android:
1. Add the following to `android/settings.gradle`:
```
include ':react-native-wkwebview'
project(':react-native-wkwebview').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-wkwebview/android')
```
2. Add the following to `build.gradle`:
```
dependencies {
    ...
    compile project(':react-native-wkwebview')
}
```
You should add the to your main project.
3. Add the following to android/app/src/main/java/**/MainApplication.java
```java
import io.cmichel.boilerplate.Package;  // add this for react-native-android-library-boilerplate

public class MainApplication extends Application implements ReactApplication {

    protected List<ReactPackage> getPackages() {
    // Add additional packages you require here
    // No need to add RnnPackage and MainReactPackage
    return Arrays.<ReactPackage>asList(
        // eg. new VectorIconsPackage()
        ...
        new DAppBrowserPackage()
    );
  }

}
```

### Usage

```js
import WKWebView from 'react-native-wkwebview';

export default class Example extends Component {
  render() {
    return (
        <WkWebView style={{ backgroundColor: '#ff0000' }}
          contentInsetAdjustmentBehavior="always"
          userAgent="MyFancyWebView"
          hideKeyboardAccessoryView={false}
          ref={(c) => this.webview = c}
          sendCookies={true}
          source={{ uri: 'https://example.org/' }}
          onMessage={(e) => console.log(e.nativeEvent)}
          injectedJavaScript="window.postMessage('Hello from WkWebView');"
          initialJavaScript="alert('alert this at document starting')"
        />
    );
  }
}
```

Try replacing your existing `WebView` with `WKWebView` and it should work in most cases. 

**Thanks Ruoyu Sun for `react-native-wkwebview-reborn` and TrustWallet for `[Web3View](https://github.com/TrustWallet/Web3View)`**


