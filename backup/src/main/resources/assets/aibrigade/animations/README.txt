Bot Animations Directory
========================

Place animation files in this directory for use with GeckoLib or AnimationAPI.

For GeckoLib (.geo.json files):
- bot_model.geo.json - Bot entity model
- bot_animations.animation.json - Animation definitions

Required animations:
- idle: Standing still with ambient movements
- walk: Normal walking animation
- run: Sprinting/running animation
- jump: Jumping animation
- attack: Melee attack swing
- damaged: Hit/damage reaction
- climb: Climbing blocks/ladders
- swim: Swimming animation
- sneak: Crouching/sneaking animation

For AnimationAPI (.json files):
- Follow AnimationAPI format specification

Animation Timing:
- idle: Loop, 60-120 ticks
- walk: Loop, speed sync with movement
- run: Loop, 1.5x walk speed
- jump: One-shot, 15-20 ticks
- attack: One-shot, 10-15 ticks
- damaged: One-shot, 5-10 ticks
- climb: Loop, sync with climbing
- swim: Loop, sync with swimming
- sneak: Loop, 0.7x walk speed

For more information:
- GeckoLib: https://geckolib.com/
- AnimationAPI: https://github.com/iLexiconn/LLibrary
