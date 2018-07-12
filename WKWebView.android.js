import React, { cloneElement } from 'react';
import { WebView, UIManager, requireNativeComponent } from 'react-native';
import PropTypes from 'prop-types';

export default class WKWebView extends WebView {

  static displayName = 'WKWebView';

  static propTypes = {
    ...WebView.propTypes,
    injectJavaScript: PropTypes.string,
  };

  goForward = () => {
    UIManager.dispatchViewManagerCommand(
      this.getWebViewHandle(),
      UIManager.RCTDAppBrowser.Commands.goForward,
      null
    );
  };

  goBack = () => {
    UIManager.dispatchViewManagerCommand(
      this.getWebViewHandle(),
      UIManager.RCTDAppBrowser.Commands.goBack,
      null
    );
  };

  reload = () => {
    UIManager.dispatchViewManagerCommand(
      this.getWebViewHandle(),
      UIManager.RCTDAppBrowser.Commands.reload,
      null
    );
  };

  stopLoading = () => {
    UIManager.dispatchViewManagerCommand(
      this.getWebViewHandle(),
      UIManager.RCTDAppBrowser.Commands.stopLoading,
      null
    );
  };

  postMessage = (data) => {
    UIManager.dispatchViewManagerCommand(
      this.getWebViewHandle(),
      UIManager.RCTDAppBrowser.Commands.postMessage,
      [String(data)]
    );
  };

  injectJavaScript = (data) => {
    UIManager.dispatchViewManagerCommand(
      this.getWebViewHandle(),
      UIManager.RCTDAppBrowser.Commands.injectJavaScript,
      [data]
    );
  };

  _onLoadingError = (event) => {
    event.persist(); // persist this event because we need to store it
    var { onError, onLoadEnd } = this.props;
    var result = onError && onError(event);
    onLoadEnd && onLoadEnd(event);
    console.warn('Encountered an error loading page', event.nativeEvent);

    result !== false && this.setState({
      lastErrorEvent: event.nativeEvent,
      viewState: 'ERROR'
    });
  };

  onLoadingError = (event) => {
    this._onLoadingError(event)
  };

  render() {
    const wrapper = super.render();
    const [webview, ...children] = wrapper.props.children;

    const DAppBrowser = (
      <RCTDAppBrowser
        {...webview.props}
        injectJavaScript={this.props.injectJavaScript}
        ref="webview"

      />
    );

    return cloneElement(wrapper, wrapper.props, DAppBrowser, ...children);
  }
}

const RCTDAppBrowser = requireNativeComponent('RCTDAppBrowser')