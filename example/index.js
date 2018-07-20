import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View
} from 'react-native';
import {
  createStackNavigator,
} from 'react-navigation';
import Example from './example';
import About from './about';

const App = createStackNavigator({
  Home: { screen: Example },
  About: { screen: About },
},
{
    initialRouteName: 'Home',
  }
);


AppRegistry.registerComponent('wkwebviewexample', () => App);
