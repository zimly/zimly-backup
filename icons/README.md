# Howto

In Android Studio:
* Navigate to `app/src/res`
* File > New > Vector Asset
  * Import SVG
* File > New > Image Asset
  * Select foreground or background
  * Scale if needed
* Generates all resources
  * Move foreground back into `drawable-v24` if needed 
  * For monochrome version: copy the foreground version into drawable and adapt colors. 
    * Had to do it this way due to needed scaling of foreground. 