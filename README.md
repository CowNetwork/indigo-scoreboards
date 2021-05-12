# indigo-scoreboards

Spigot plugin to display roles in scoreboards with Indigo.  
Just put this plugin + Grape and Indigo in the `plugins` folder and bam.

# Configuration

Example `config.yml`:

```yaml
defaultColor: "FFFFFF"
teams:
  - role: admin
    prefix: "%roleColor%admin "
    color: "808080"
    suffix: ""
```

Important to notice is that in general colors have to be a hex string. Exception is prefix and suffix, as you can use normal chat color codes in these strings.  
Use `%roleColor%` to automatically insert the role's color into the prefix/suffix.
