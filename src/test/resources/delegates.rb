require "json"

class CustomDelegate

  attr_accessor :context

  def deserialize_meta_identifier(meta_identifier)
    reversed_meta_id = meta_identifier.reverse
    matches          = /^((?<sc>\d+:\d+);)?((?<pg>\d+);)?(?<id>.+)/.match(reversed_meta_id)
    captures         = matches.named_captures
    struct                     = {}
    struct['identifier']       = captures['id'].reverse
    struct['page_number']      = captures['pg'].reverse.to_i if captures['pg']
    struct['scale_constraint'] = captures['sc'].reverse.split(':').map(&:to_i) if captures['sc']
    struct
  end

  def serialize_meta_identifier(components)
    [
      components['identifier'],
      components['page_number'],
      components['scale_constraint']&.join(':')
    ].reject(&:nil?).join(';')
  end

  def authorize_before_access(options = {})
    case context['identifier']
    when "allowed.jpg"
      true
    when "forbidden-boolean.jpg"
      false
    when 'redirect'
      return {
        'status_code' => 303,
        'location'    => "http://example.org/"
      }
    end
  end

  def authorize(options = {})
    case context['identifier']
    when "allowed.jpg"
      true
    when "forbidden-boolean.jpg"
      false
    when "redirect"
      {
        'location'    => "http://example.org/",
        'status_code' => 303
      }
    when "error"
      raise "Error"
    end
  end

  def customize_iiif1_information_response(info)
    info['new_key'] = "new value"
  end

  def customize_iiif2_information_response(info)
    info['new_key'] = "new value"
  end

  def customize_iiif3_information_response(info)
    info['new_key'] = "new value"
  end

  def source(options = {})
    case context['identifier']
    when 'bogus'
      return nil
    else
      return 'FilesystemSource'
    end
  end

  def filesystemsource_pathname(options = {})
    case context['identifier']
    when 'missing'
      nil
    else
      context['identifier']
    end
  end

  def httpsource_resource_info(options = {})
    case context['identifier']
    when 'string'
      return 'http://example.org/foxes'
    when 'hash'
      return { 'uri' => 'http://example.org/birds' }
    end
  end

  def overlay(options = {})
    case context['identifier']
    when 'image'
      return {
        'image' => '/dev/cats',
        'inset' => 5,
        'position' => 'bottom left'
      }
    when 'string'
      return {
        'background_color' => 'rgba(12, 23, 34, 45)',
        'string' => "dogs\ndogs",
        'inset' => 5,
        'position' => 'bottom left',
        'color' => 'red',
        'font' => 'SansSerif',
        'font_size' => 20,
        'font_min_size' => 11,
        'font_weight' => 1.5,
        'glyph_spacing' => 0.1,
        'stroke_color' => 'blue',
        'stroke_width' => 3,
        'word_wrap' => false
      }
    end
  end

  def redactions(options = {})
    case context['identifier']
    when 'empty'
      return []
    when 'redacted'
      return [
        {
          'x'      => 0,
          'y'      => 10,
          'width'  => 50,
          'height' => 70,
          'color'  => 'black'
        }
      ]
    end
  end

  def metadata(options = {})
    case context['identifier']
    when 'metadata'
      return '<rdf:RDF>variant metadata</rdf:RDF>'
    else
      return nil
    end
  end

end
